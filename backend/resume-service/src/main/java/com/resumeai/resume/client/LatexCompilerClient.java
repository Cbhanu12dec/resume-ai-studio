package com.resumeai.resume.client;

import com.resumeai.common.dto.ParsedResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class LatexCompilerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${latex-service.base-url:http://localhost:8083}")
    private String latexServiceBaseUrl;

    public String generateLatex(ParsedResumeData parsedData) {
        var request = Map.of("parsedData", parsedData, "template", "default");

        return webClientBuilder.baseUrl(latexServiceBaseUrl).build()
                .post()
                .uri("/api/v1/latex/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GenerateResponseWrapper.class)
                .timeout(Duration.ofSeconds(60))
                .block()
                .getData()
                .getLatex();
    }

    public byte[] compilePdf(String latexSource) {
        return webClientBuilder.baseUrl(latexServiceBaseUrl).build()
                .post()
                .uri("/api/v1/latex/compile")
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue(latexSource)
                .retrieve()
                .bodyToMono(byte[].class)
                .timeout(Duration.ofSeconds(60))
                .block();
    }

    // ─── Wrappers ─────────────────────────────────────────────────────────────

    @lombok.Data
    public static class LatexResult {
        private String latex;
    }

    @lombok.Data
    public static class GenerateResponseWrapper {
        private boolean success;
        private LatexResult data;
    }
}
