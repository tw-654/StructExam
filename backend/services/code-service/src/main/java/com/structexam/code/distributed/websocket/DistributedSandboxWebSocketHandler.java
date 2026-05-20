package com.structexam.code.distributed.websocket;

import com.structexam.code.distributed.config.DistributedJudgeProperties;
import com.structexam.code.distributed.dto.InteractiveSessionView;
import com.structexam.code.distributed.service.SandboxNodeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class DistributedSandboxWebSocketHandler extends TextWebSocketHandler {

    private static final Logger logger = LoggerFactory.getLogger(DistributedSandboxWebSocketHandler.class);

    private final SandboxNodeRegistry nodeRegistry;
    private final DistributedJudgeProperties properties;
    private final StandardWebSocketClient webSocketClient = new StandardWebSocketClient();
    private final Map<String, SessionBinding> bindings = new ConcurrentHashMap<>();

    public DistributedSandboxWebSocketHandler(SandboxNodeRegistry nodeRegistry,
                                              DistributedJudgeProperties properties) {
        this.nodeRegistry = nodeRegistry;
        this.properties = properties;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession gatewaySession) {
        logger.info("Distributed sandbox gateway connected: {}", gatewaySession.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession gatewaySession, TextMessage message) throws Exception {
        SessionBinding existing = bindings.get(gatewaySession.getId());
        if (existing != null && existing.sandboxSession.isOpen()) {
            existing.sandboxSession.sendMessage(message);
            return;
        }

        Optional<ServiceInstance> nodeOptional = nodeRegistry.selectLeastTasks();
        if (nodeOptional.isEmpty()) {
            sendError(gatewaySession, "No healthy sandbox node available");
            return;
        }

        ServiceInstance node = nodeOptional.get();
        URI sandboxUri = toWebSocketUri(node.getUri(), properties.getSandboxWebSocketPath());
        nodeRegistry.incrementRunningTasks(node);

        try {
            WebSocketSession sandboxSession = webSocketClient
                    .doHandshake(new SandboxRelayHandler(gatewaySession, node), new WebSocketHttpHeaders(), sandboxUri)
                    .get();
            bindings.put(gatewaySession.getId(), new SessionBinding(sandboxSession, node));
            sandboxSession.sendMessage(message);
            logger.info("Bound gateway session {} to sandbox {}", gatewaySession.getId(), sandboxUri);
        } catch (Exception ex) {
            nodeRegistry.decrementRunningTasks(node);
            nodeRegistry.markFailure(node);
            sendError(gatewaySession, "Failed to connect sandbox node: " + ex.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession gatewaySession, CloseStatus status) throws Exception {
        SessionBinding binding = bindings.remove(gatewaySession.getId());
        if (binding != null) {
            try {
                if (binding.sandboxSession.isOpen()) {
                    binding.sandboxSession.close(status);
                }
            } finally {
                nodeRegistry.decrementRunningTasks(binding.node);
            }
        }
        logger.info("Distributed sandbox gateway closed: {}, status: {}", gatewaySession.getId(), status);
    }

    private URI toWebSocketUri(URI baseUri, String path) {
        String scheme = "https".equalsIgnoreCase(baseUri.getScheme()) ? "wss" : "ws";
        URI resolved = baseUri.resolve(path);
        return URI.create(scheme + "://" + resolved.getAuthority() + resolved.getPath());
    }

    private void sendError(WebSocketSession session, String message) throws IOException {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage("{\"type\":\"ERROR\",\"data\":\"" + escapeJson(message) + "\"}"));
        }
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public List<InteractiveSessionView> activeSessions() {
        return bindings.entrySet().stream()
                .map(entry -> {
                    InteractiveSessionView view = new InteractiveSessionView();
                    view.setGatewaySessionId(entry.getKey());
                    view.setSandboxUri(entry.getValue().node.getUri());
                    view.setStatus(entry.getValue().sandboxSession.isOpen() ? "OPEN" : "CLOSED");
                    return view;
                })
                .collect(Collectors.toList());
    }

    private class SandboxRelayHandler extends TextWebSocketHandler {
        private final WebSocketSession gatewaySession;
        private final ServiceInstance node;

        private SandboxRelayHandler(WebSocketSession gatewaySession, ServiceInstance node) {
            this.gatewaySession = gatewaySession;
            this.node = node;
        }

        @Override
        protected void handleTextMessage(WebSocketSession sandboxSession, TextMessage message) throws Exception {
            if (gatewaySession.isOpen()) {
                gatewaySession.sendMessage(message);
            }
        }

        @Override
        public void afterConnectionClosed(WebSocketSession sandboxSession, CloseStatus status) throws Exception {
            bindings.remove(gatewaySession.getId());
            nodeRegistry.decrementRunningTasks(node);
            if (gatewaySession.isOpen()) {
                gatewaySession.close(status);
            }
        }

        @Override
        public void handleTransportError(WebSocketSession sandboxSession, Throwable exception) throws Exception {
            nodeRegistry.markFailure(node);
            sendError(gatewaySession, "Sandbox transport error: " + exception.getMessage());
        }
    }

    private static class SessionBinding {
        private final WebSocketSession sandboxSession;
        private final ServiceInstance node;

        private SessionBinding(WebSocketSession sandboxSession, ServiceInstance node) {
            this.sandboxSession = sandboxSession;
            this.node = node;
        }
    }
}
