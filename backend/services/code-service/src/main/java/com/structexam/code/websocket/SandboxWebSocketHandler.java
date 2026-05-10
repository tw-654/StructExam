package com.structexam.code.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.structexam.code.sandbox.InteractiveProcessManager;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SandboxWebSocketHandler extends org.springframework.web.socket.handler.TextWebSocketHandler {

    private static final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static InteractiveProcessManager processManager;

    public static void setProcessManager(InteractiveProcessManager manager) {
        processManager = manager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        System.out.println("WebSocket connected: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        System.out.println("Received message from session: " + sessionId + ", payload: " + message.getPayload());

        if (processManager == null) {
            sendError(session, "Sandbox service not initialized");
            return;
        }

        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message.getPayload(), WebSocketMessage.class);
            String type = wsMessage.getType();

            if (type == null) {
                sendError(session, "Unknown message type");
                return;
            }

            switch (type) {
                case "START":
                    handleStart(session, wsMessage);
                    break;
                case "INPUT":
                    handleInput(session, wsMessage);
                    break;
                case "TERMINATE":
                    handleTerminate(session, wsMessage);
                    break;
                default:
                    sendError(session, "Unknown message type: " + type);
            }
        } catch (JsonMappingException e) {
            System.err.println("JSON parse error: " + e.getMessage());
            sendError(session, "Invalid message format");
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "Error: " + e.getMessage());
        }
    }

    private void handleStart(WebSocketSession session, WebSocketMessage message) throws IOException {
        String sessionId = session.getId();
        String code = message.getCode();
        String language = message.getLanguage();
        Long timeout = message.getTimeout();

        if (code == null || code.isEmpty()) {
            sendError(session, "Code is required");
            return;
        }

        if (language == null || language.isEmpty()) {
            language = "python";
        }

        if (timeout == null || timeout <= 0) {
            timeout = 60L;
        }

        System.out.println("Starting process for session: " + sessionId + ", language: " + language);

        try {
            sendMessage(session, "STATUS", sessionId, "STARTING", "正在初始化...");

            InteractiveProcessManager.InteractiveProcess process =
                    processManager.startProcess(sessionId, code, language, timeout);

            if (process == null) {
                sendError(session, "Failed to create process");
                return;
            }

            final String sid = sessionId;
            process.setOutputListener((outputSid, data, isError) -> {
                WebSocketSession s = sessions.get(outputSid);
                if (s != null && s.isOpen()) {
                    try {
                        sendMessage(s, isError ? "ERROR" : "OUTPUT", outputSid, null, data);
                    } catch (IOException ignored) {}
                }
            });

            process.setStatusListener((statusSid, status) -> {
                WebSocketSession s = sessions.get(statusSid);
                if (s != null && s.isOpen()) {
                    try {
                        sendMessage(s, "STATUS", statusSid, status.name(), null);
                    } catch (IOException ignored) {}
                }
            });

            process.setErrorListener((errorSid, error) -> {
                WebSocketSession s = sessions.get(errorSid);
                if (s != null && s.isOpen()) {
                    try {
                        sendMessage(s, "ERROR", errorSid, null, error);
                    } catch (IOException ignored) {}
                }
            });

            sendMessage(session, "STATUS", sessionId, "RUNNING", "进程已启动");

        } catch (Exception e) {
            System.err.println("Error starting process: " + e.getMessage());
            e.printStackTrace();
            sendError(session, "启动失败: " + e.getMessage());
        }
    }

    private void handleInput(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        String input = message.getData();

        if (input == null) {
            input = "\n";
        } else if (!input.endsWith("\n")) {
            input += "\n";
        }

        InteractiveProcessManager.InteractiveProcess process = processManager.getProcess(sessionId);
        if (process == null) {
            sendError(session, "No active process found for session");
            return;
        }

        process.writeInput(input);
    }

    private void handleTerminate(WebSocketSession session, WebSocketMessage message) {
        String sessionId = session.getId();
        processManager.terminateProcess(sessionId);
        try {
            sendMessage(session, "STATUS", sessionId, "TERMINATED", "进程已终止");
        } catch (IOException ignored) {}
    }

    private void sendMessage(WebSocketSession session, String type, String sessionId, String status, String data) throws IOException {
        WebSocketMessage msg = new WebSocketMessage();
        msg.setType(type);
        msg.setSessionId(sessionId);
        msg.setStatus(status);
        msg.setData(data);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
    }

    private void sendError(WebSocketSession session, String error) {
        try {
            sendMessage(session, "ERROR", null, null, error);
        } catch (IOException e) {
            System.err.println("Failed to send error: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        System.out.println("WebSocket closed: " + sessionId + ", status: " + status);
        if (processManager != null) {
            processManager.terminateProcess(sessionId);
        }
    }

    public static class WebSocketMessage {
        private String type;
        private String sessionId;
        private String code;
        private String language;
        private String data;
        private String status;
        private Long timeout;

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Long getTimeout() { return timeout; }
        public void setTimeout(Long timeout) { this.timeout = timeout; }
    }
}
