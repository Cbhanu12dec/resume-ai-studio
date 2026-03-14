package com.resumeai.resume.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.common.dto.ParsedResumeData;
import com.resumeai.resume.dto.ChatEditRequest;
import com.resumeai.resume.dto.ChatEditResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class AiOrchestratorClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String aiServiceBaseUrl;

    public AiOrchestratorClient(WebClient.Builder webClientBuilder,
                                @Value("${ai-service.base-url:http://localhost:8082}") String aiServiceBaseUrl,
                                ObjectMapper objectMapper) {
        this.aiServiceBaseUrl = aiServiceBaseUrl;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.baseUrl(aiServiceBaseUrl).build();
    }

    public ParseResult parseResume(byte[] fileBytes, String mediaType) {
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        var request = Map.of("fileBase64", base64, "mediaType", mediaType);

        return webClient
                .post()
                .uri("/api/v1/ai/parse")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ParseResponseWrapper.class)
                .timeout(Duration.ofSeconds(120))
                .block()
                .getData();
    }

    public ChatEditResponse editResume(ParsedResumeData parsedData, String latexSource, ChatEditRequest request) {
        var body = Map.of(
                "parsedData", parsedData,
                "latexSource", latexSource,
                "message", request.getMessage(),
                "history", request.getHistory() != null ? request.getHistory() : java.util.List.of()
        );

        return webClient
                .post()
                .uri("/api/v1/ai/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EditResponseWrapper.class)
                .timeout(Duration.ofSeconds(120))
                .block()
                .getData();
    }

    /**
     * Edit via the reliable non-streaming HTTP endpoint.
     * Used by the WebSocket handler for EDIT_RESUME intent (same path that worked before WS migration).
     */
    public ChatEditResponse editResumeRaw(ParsedResumeData parsedData, String latexSource,
                                          String message, List<Map<String, String>> history) {
        var body = new HashMap<String, Object>();
        body.put("parsedData", parsedData);
        body.put("latexSource", latexSource != null ? latexSource : "");
        body.put("message", message);
        body.put("history", history != null ? history : List.of());

        return webClient
                .post()
                .uri("/api/v1/ai/edit")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(EditResponseWrapper.class)
                .timeout(Duration.ofSeconds(120))
                .block()
                .getData();
    }

    // ─── WebSocket streaming to ai-orchestrator ───────────────────────────────

    public Flux<WsFrame> streamEditResume(ParsedResumeData parsedData, String latexSource,
                                          String message, List<Map<String, String>> history,
                                          String intent) {
        final String requestJson;
        try {
            var body = new HashMap<String, Object>();
            body.put("parsedData", parsedData);
            body.put("latexSource", latexSource != null ? latexSource : "");
            body.put("message", message);
            body.put("history", history != null ? history : List.of());
            body.put("intent", intent != null ? intent : "EDIT_RESUME");
            requestJson = objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return Flux.error(new RuntimeException("Failed to serialize WS request: " + e.getMessage()));
        }

        String wsUrl = aiServiceBaseUrl
                .replace("https://", "wss://")
                .replace("http://", "ws://") + "/ws/ai/stream";
        log.debug("Connecting to AI orchestrator WS: {}", wsUrl);

        return Flux.create(emitter -> {
            ReactorNettyWebSocketClient client = new ReactorNettyWebSocketClient();
            Disposable connection = client.execute(URI.create(wsUrl), wsSession ->
                    wsSession.send(Mono.just(wsSession.textMessage(requestJson)))
                            .thenMany(wsSession.receive()
                                    .doOnNext(msg -> {
                                        try {
                                            Map<?, ?> frame = objectMapper.readValue(
                                                    msg.getPayloadAsText(), Map.class);
                                            String type = (String) frame.get("type");
                                            String data = (String) frame.get("data");
                                            emitter.next(new WsFrame(type, data));
                                            if ("done".equals(type) || "error".equals(type)) {
                                                emitter.complete();
                                            }
                                        } catch (Exception e) {
                                            log.error("Failed to parse AI WS frame: {}", e.getMessage());
                                            emitter.error(e);
                                        }
                                    })
                                    .doOnComplete(emitter::complete)
                                    .doOnError(emitter::error))
                            .then()
            ).subscribe(null, emitter::error, emitter::complete);

            emitter.onDispose(() -> {
                if (!connection.isDisposed()) {
                    connection.dispose();
                }
            });
        });
    }

    // ─── Types ────────────────────────────────────────────────────────────────

    public record WsFrame(String type, String data) {}

    // ─── Response Wrappers ────────────────────────────────────────────────────

    @lombok.Data
    public static class ParseResult {
        private ParsedResumeData parsedData;
    }

    @lombok.Data
    public static class ParseResponseWrapper {
        private boolean success;
        private ParseResult data;
    }

    @lombok.Data
    public static class EditResponseWrapper {
        private boolean success;
        private ChatEditResponse data;
    }
}
