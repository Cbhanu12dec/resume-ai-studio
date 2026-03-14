package com.resumeai.ai.client;

import com.resumeai.ai.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClaudeApiClient {

    private final WebClient claudeWebClient;

    @Value("${anthropic.model:claude-sonnet-4-20250514}")
    private String model;

    @Value("${anthropic.max-tokens.parse:8192}")
    private int parseMaxTokens;

    @Value("${anthropic.max-tokens.edit:16000}")
    private int editMaxTokens;

    @Value("${anthropic.max-tokens.match:2048}")
    private int matchMaxTokens;

    // ─── Parse Resume ─────────────────────────────────────────────────────────

    public Mono<ClaudeResponse> parseResume(byte[] fileBytes, String mediaType) {
        String base64 = Base64.getEncoder().encodeToString(fileBytes);
        // PDFs use "document" type; images (PNG, JPEG…) use "image" type
        ClaudeContent fileContent = (mediaType != null && mediaType.contains("pdf"))
                ? ClaudeContent.document("application/pdf", base64)
                : ClaudeContent.image(mediaType, base64);

        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(parseMaxTokens)
                .system("You are a precise resume parser. Return ONLY valid JSON with no markdown fences. " +
                        "Extract all resume information into the exact schema provided.")
                .messages(List.of(
                        ClaudeMessage.user(List.of(
                                fileContent,
                                ClaudeContent.text(buildParsePrompt())
                        ))
                ))
                .build();
        return callApi(request);
    }

    // ─── Edit Resume ──────────────────────────────────────────────────────────

    public Mono<ClaudeResponse> editResume(String parsedJson, String latexSource,
                                            String userMessage, List<Map<String, String>> history) {
        StringBuilder userContent = new StringBuilder();
        userContent.append("Current JSON:\n").append(parsedJson)
                .append("\n\nCurrent LaTeX:\n").append(latexSource)
                .append("\n\nUser request: ").append(userMessage);

        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(editMaxTokens)
                .system("You are an expert resume editor. Return ONLY valid JSON with this exact structure: " +
                        "{\"updated_parsed\": {...}, \"updated_latex\": \"...\", \"message\": \"...\"}")
                .messages(buildConversationHistory(history, userContent.toString()))
                .build();
        return callApi(request);
    }

    // ─── Match Resume ─────────────────────────────────────────────────────────

    public Mono<ClaudeResponse> matchResume(String resumeJson, String jobDescription) {
        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(matchMaxTokens)
                .system("You are a resume-to-JD matcher. Return ONLY valid JSON with no markdown fences.")
                .messages(List.of(
                        ClaudeMessage.user(List.of(
                                ClaudeContent.text(
                                        "Resume JSON:\n" + resumeJson +
                                        "\n\nJob Description:\n" + jobDescription +
                                        "\n\nReturn JSON: {\"score\": 0-100, " +
                                        "\"breakdown\": {\"technical\": 0-100, \"experience\": 0-100, " +
                                        "\"education\": 0-100, \"keywords\": 0-100, \"industry\": 0-100}, " +
                                        "\"matching_keywords\": [], \"missing_keywords\": [], \"suggestions\": []}"
                                )
                        ))
                ))
                .build();
        return callApi(request);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private Mono<ClaudeResponse> callApi(ClaudeRequest request) {
        return claudeWebClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ClaudeResponse.class)
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(e -> e.getMessage() != null &&
                                (e.getMessage().contains("429") || e.getMessage().contains("503"))))
                .doOnError(e -> log.error("Claude API call failed: {}", e.getMessage()));
    }

    private String buildParsePrompt() {
        return "Extract all resume data and return ONLY this JSON structure:\n" +
               "{\n" +
               "  \"name\": \"\",\n" +
               "  \"contact\": {\"email\": \"\", \"phone\": \"\", \"location\": \"\", " +
               "\"linkedin\": \"\", \"github\": \"\", \"website\": \"\"},\n" +
               "  \"summary\": \"\",\n" +
               "  \"experience\": [{\"company\": \"\", \"title\": \"\", \"startDate\": \"\", " +
               "\"endDate\": \"\", \"location\": \"\", \"bullets\": []}],\n" +
               "  \"education\": [{\"institution\": \"\", \"degree\": \"\", \"field\": \"\", " +
               "\"graduationDate\": \"\", \"gpa\": \"\"}],\n" +
               "  \"skillCategories\": [{\"category\": \"Languages\", \"skills\": []}, " +
               "{\"category\": \"Frameworks & Libraries\", \"skills\": []}, " +
               "{\"category\": \"Tools & Platforms\", \"skills\": []}],\n" +
               "  \"skills\": [],\n" +
               "  \"projects\": [{\"name\": \"\", \"description\": \"\", " +
               "\"technologies\": [], \"url\": \"\", \"bullets\": []}],\n" +
               "  \"certifications\": []\n" +
               "}\n" +
               "For skillCategories: group skills into meaningful categories (e.g. Languages, Frameworks & Libraries, " +
               "Databases, Cloud & DevOps, Tools). Only include categories that have actual skills. " +
               "Also populate the flat skills[] array with all skills combined for backward compatibility.";
    }

    private List<ClaudeMessage> buildConversationHistory(
            List<Map<String, String>> history, String currentMessage) {
        var messages = new java.util.ArrayList<ClaudeMessage>();
        if (history != null) {
            history.forEach(h -> {
                String role = h.get("role");
                String content = h.get("content");
                if ("user".equals(role)) {
                    messages.add(ClaudeMessage.user(List.of(ClaudeContent.text(content))));
                } else {
                    messages.add(ClaudeMessage.assistant(content));
                }
            });
        }
        messages.add(ClaudeMessage.user(List.of(ClaudeContent.text(currentMessage))));
        return messages;
    }
}
