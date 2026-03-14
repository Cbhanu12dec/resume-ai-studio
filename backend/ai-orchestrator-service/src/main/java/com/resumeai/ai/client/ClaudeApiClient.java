package com.resumeai.ai.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
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
    private final ObjectMapper objectMapper;

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
                .system("You are an expert resume editor. Return ONLY valid JSON — no markdown fences, no extra text.\n" +
                        "JSON structure: {\"updated_parsed\": {...}, \"updated_latex\": \"...\", \"message\": \"...\"}\n" +
                        "LaTeX format rules (ATS-friendly plain black style):\n" +
                        "- \\documentclass[10.5pt,letterpaper]{article} with geometry margins top/bottom 0.5in, left/right 0.65in\n" +
                        "- Packages: titlesec, hyperref, enumitem, microtype. NO fontawesome, NO xcolor.\n" +
                        "- Section headings: \\titleformat{\\section}{\\large\\bfseries}{}{0em}{}[\\vspace{1pt}\\titlerule\\vspace{2pt}]\n" +
                        "- Bullet lists: \\setlist[itemize]{leftmargin=1.2em,itemsep=1pt,parsep=0pt,topsep=2pt,label=\\textbullet}\n" +
                        "- Header: centered name in {\\Huge\\bfseries}, contact on next line separated by \\ $|$\\\n" +
                        "- Section order: Summary, Education, Technical Skills (as itemize with \\textbf{Category:}), Experience, Projects, Certifications\n" +
                        "- hypersetup{colorlinks=false}. All text plain black — no \\color{} commands.\n" +
                        "- In the LaTeX string, escape ALL backslashes as \\\\ (e.g. \\\\documentclass, \\\\begin, \\\\end).\n" +
                        "- Output COMPLETE LaTeX from \\documentclass to \\end{document} — never truncate.")
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

    // ─── Streaming: Resume Edit ───────────────────────────────────────────────
    // Claude outputs: "<chat reply>\n---EDIT---\n{json}"
    // Tokens before ---EDIT--- are streamed live; JSON after is accumulated.

    public Flux<String> streamEditResume(String parsedJson, String latexSource,
                                         String userMessage, List<Map<String, String>> history) {
        String userContent = "Current resume JSON:\n" + parsedJson +
                "\n\nCurrent LaTeX:\n" + (latexSource != null ? latexSource : "") +
                "\n\nUser request: " + userMessage;

        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(editMaxTokens)
                .stream(true)
                .system("You are an expert resume editor and assistant.\n" +
                        "Respond in exactly TWO parts:\n" +
                        "PART 1: One concise sentence — either confirming what you changed, or answering the user's question.\n" +
                        "PART 2: Write the separator ---EDIT--- on its own line, then immediately write ONLY valid JSON (no markdown fences):\n" +
                        "{\"updated_parsed\": <full resume JSON object>, \"updated_latex\": \"<full LaTeX source>\"}\n" +
                        "Critical rules:\n" +
                        "- ALWAYS output ---EDIT--- and the full JSON.\n" +
                        "- If the user requested a change, apply it and output the UPDATED resume data.\n" +
                        "- If the user asked a question (no change requested), output the CURRENT UNCHANGED resume data.\n" +
                        "- Output the COMPLETE JSON and COMPLETE LaTeX — never truncate or omit sections.\n" +
                        "- In the LaTeX string, escape ALL backslashes as \\\\ (e.g. \\\\documentclass, \\\\begin, \\\\end).\n" +
                        "- Never include 'message' inside the JSON. Never use markdown code fences.")
                .messages(buildConversationHistory(history, userContent))
                .build();
        return streamApi(request);
    }

    // ─── Streaming: Chat Only ─────────────────────────────────────────────────

    public Flux<String> streamChatOnly(String parsedJson, String userMessage,
                                       List<Map<String, String>> history) {
        String userContent = "Resume data:\n" + parsedJson + "\n\nUser question: " + userMessage;

        ClaudeRequest request = ClaudeRequest.builder()
                .model(model)
                .maxTokens(512)
                .stream(true)
                .system("You are a helpful resume assistant. " +
                        "Answer the user's question about their resume in 1-3 sentences. " +
                        "Be conversational and concise. Do NOT output any JSON or ---EDIT---.")
                .messages(buildConversationHistory(history, userContent))
                .build();
        return streamApi(request);
    }

    // ─── Internal ─────────────────────────────────────────────────────────────

    private Flux<String> streamApi(ClaudeRequest request) {
        return claudeWebClient.post()
                .uri("/v1/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line.startsWith("data: "))
                .map(line -> line.substring(6))
                .filter(json -> !json.equals("[DONE]"))
                .mapNotNull(json -> {
                    try {
                        var node = objectMapper.readTree(json);
                        String type = node.path("type").asText();
                        if ("content_block_delta".equals(type)) {
                            var delta = node.path("delta");
                            if ("text_delta".equals(delta.path("type").asText())) {
                                return delta.path("text").asText(null);
                            }
                        } else if ("error".equals(type)) {
                            String errMsg = node.path("error").path("message").asText("Claude API error");
                            log.error("Claude SSE error: {}", errMsg);
                            throw new RuntimeException("Claude API error: " + errMsg);
                        }
                        return null;
                    } catch (RuntimeException e) {
                        throw e;
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(text -> text != null && !text.isEmpty())
                .doOnError(e -> log.error("Claude streaming error: {}", e.getMessage()));
    }

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
