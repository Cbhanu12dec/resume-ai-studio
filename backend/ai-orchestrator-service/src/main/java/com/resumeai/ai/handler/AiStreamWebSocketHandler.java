package com.resumeai.ai.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.dto.EditRequest;
import com.resumeai.ai.service.AiOrchestratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reactive WebSocket handler for AI streaming (ai-orchestrator-service).
 *
 * URL:     ws://localhost:8082/ws/ai/stream
 * Client → server:  First frame = serialized EditRequest JSON
 *                   Subsequent frames = {"type":"cancel"} to abort
 * Server → client:  {"type":"token","data":"word"}
 *                   {"type":"done","data":"{EditResponse JSON}"}
 *                   {"type":"error","data":"reason"}
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiStreamWebSocketHandler implements WebSocketHandler {

    private final AiOrchestratorService aiService;
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Sinks.Empty<Void> cancelSink = Sinks.empty();
        AtomicBoolean firstMessage = new AtomicBoolean(true);
        Sinks.One<EditRequest> requestSink = Sinks.one();

        // Single subscription handles both the request and any cancel signals
        session.receive()
                .doOnNext(msg -> {
                    try {
                        String text = msg.getPayloadAsText();
                        if (firstMessage.getAndSet(false)) {
                            EditRequest req = objectMapper.readValue(text, EditRequest.class);
                            log.info("AI WS stream — session={} intent={}", session.getId(), req.getIntent());
                            requestSink.tryEmitValue(req);
                        } else {
                            Map<?, ?> frame = objectMapper.readValue(text, Map.class);
                            if ("cancel".equals(frame.get("type"))) {
                                log.info("AI WS cancel — session={}", session.getId());
                                cancelSink.tryEmitEmpty();
                            }
                        }
                    } catch (Exception e) {
                        log.error("AI WS inbound parse error — session={}: {}", session.getId(), e.getMessage());
                        requestSink.tryEmitError(e);
                    }
                })
                .doOnError(e -> requestSink.tryEmitError(e))
                .doOnComplete(() -> cancelSink.tryEmitEmpty())
                .subscribe(null, e -> {}, () -> {});

        return requestSink.asMono()
                .flatMapMany(request ->
                        aiService.streamEditWs(request)
                                .takeUntilOther(cancelSink.asMono())
                                .onErrorResume(e -> {
                                    log.error("AI stream error — session={}", session.getId(), e);
                                    return Flux.just(frame("error",
                                            e.getMessage() != null ? e.getMessage() : "Stream failed"));
                                })
                )
                .map(session::textMessage)
                .as(session::send)
                .doFinally(sig ->
                        log.debug("AI WS session closed — id={} signal={}", session.getId(), sig));
    }

    private String frame(String type, String data) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "data", data));
        } catch (Exception e) {
            return "{\"type\":\"error\",\"data\":\"serialization_error\"}";
        }
    }
}
