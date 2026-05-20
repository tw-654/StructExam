package com.structexam.sandbox.config;

import com.structexam.sandbox.websocket.SandboxNodeWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class SandboxWebSocketConfig implements WebSocketConfigurer {

    private final SandboxNodeWebSocketHandler handler;

    public SandboxWebSocketConfig(SandboxNodeWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler, "/ws/sandbox").setAllowedOrigins("*");
    }
}
