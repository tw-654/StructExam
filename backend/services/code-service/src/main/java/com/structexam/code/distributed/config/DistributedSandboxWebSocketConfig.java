package com.structexam.code.distributed.config;

import com.structexam.code.distributed.websocket.DistributedSandboxWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
public class DistributedSandboxWebSocketConfig implements WebSocketConfigurer {

    private final DistributedSandboxWebSocketHandler handler;

    public DistributedSandboxWebSocketConfig(DistributedSandboxWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/distributed-sandbox")
                .setAllowedOrigins("*");
    }
}
