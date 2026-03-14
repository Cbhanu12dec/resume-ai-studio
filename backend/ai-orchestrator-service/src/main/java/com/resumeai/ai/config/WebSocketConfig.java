package com.resumeai.ai.config;

import com.resumeai.ai.handler.AiStreamWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.Map;

/**
 * Registers the reactive WebSocket endpoint at /ws/ai/stream.
 * ai-orchestrator-service runs on Spring WebFlux (Netty), so we use the
 * reactive WebSocket API — NOT the Servlet-based @EnableWebSocket.
 */
@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {

    private final AiStreamWebSocketHandler aiStreamWebSocketHandler;

    @Bean
    public HandlerMapping webSocketHandlerMapping() {
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(Map.of("/ws/ai/stream", (WebSocketHandler) aiStreamWebSocketHandler));
        mapping.setOrder(-1); // higher priority than default DispatcherHandler
        return mapping;
    }

    @Bean
    public WebSocketHandlerAdapter webSocketHandlerAdapter() {
        return new WebSocketHandlerAdapter();
    }
}
