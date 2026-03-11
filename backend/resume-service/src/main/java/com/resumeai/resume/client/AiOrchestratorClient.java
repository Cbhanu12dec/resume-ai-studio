package com.resumeai.resume.client;

import com.resumeai.common.dto.ParsedResumeData;
import com.resumeai.resume.dto.ChatEditRequest;
import com.resumeai.resume.dto.ChatEditResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Base64;
import java.util.Map;

@Component
@Slf4j
public class AiOrchestratorClient {

    private final WebClient webClient;

    public AiOrchestratorClient(WebClient.Builder webClientBuilder,
                                @Value("${ai-service.base-url:http://localhost:8082}") String aiServiceBaseUrl) {
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
