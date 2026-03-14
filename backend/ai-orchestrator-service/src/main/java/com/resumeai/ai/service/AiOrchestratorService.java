package com.resumeai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.client.ClaudeApiClient;
import com.resumeai.ai.dto.*;
import com.resumeai.common.dto.ParsedResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiOrchestratorService {

    private final ClaudeApiClient claudeApiClient;
    private final ObjectMapper objectMapper;

    public Mono<ParseResponse> parseResume(ParseRequest request) {
        byte[] fileBytes = Base64.getDecoder().decode(request.getFileBase64());
        return claudeApiClient.parseResume(fileBytes, request.getMediaType())
                .map(response -> {
                    String json = stripMarkdown(response.getTextContent());
                    try {
                        ParsedResumeData parsed = objectMapper.readValue(json, ParsedResumeData.class);
                        return ParseResponse.builder().parsedData(parsed).build();
                    } catch (Exception e) {
                        log.error("Failed to parse Claude response as JSON: {}", json, e);
                        throw new RuntimeException("AI returned invalid JSON", e);
                    }
                });
    }

    public Mono<EditResponse> editResume(EditRequest request) {
        String parsedJson = safeToJson(request.getParsedData());
        return claudeApiClient.editResume(parsedJson, request.getLatexSource(),
                request.getMessage(), request.getHistory())
                .map(response -> {
                    String json = stripMarkdown(response.getTextContent());
                    try {
                        var map = objectMapper.readValue(json, Map.class);
                        ParsedResumeData updatedParsed = objectMapper.convertValue(
                                map.get("updated_parsed"), ParsedResumeData.class);
                        return EditResponse.builder()
                                .updatedParsedData(updatedParsed)
                                .updatedLatex((String) map.get("updated_latex"))
                                .message((String) map.get("message"))
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to parse edit response: {}", json, e);
                        throw new RuntimeException("AI returned invalid edit response", e);
                    }
                });
    }

    public Mono<MatchResponse> matchResume(MatchRequest request) {
        String resumeJson = safeToJson(request.getResumeData());
        return claudeApiClient.matchResume(resumeJson, request.getJobDescription())
                .map(response -> {
                    String json = stripMarkdown(response.getTextContent());
                    try {
                        var map = objectMapper.readValue(json, Map.class);
                        return MatchResponse.builder()
                                .score((Integer) map.get("score"))
                                .breakdown((Map<String, Integer>) map.get("breakdown"))
                                .matchingKeywords((List<String>) map.get("matching_keywords"))
                                .missingKeywords((List<String>) map.get("missing_keywords"))
                                .suggestions((List<String>) map.get("suggestions"))
                                .build();
                    } catch (Exception e) {
                        log.error("Failed to parse match response: {}", json, e);
                        throw new RuntimeException("AI returned invalid match response", e);
                    }
                });
    }

    // ── WebSocket streaming pipeline ─────────────────────────────────────────
    // Returns Flux<String> where each element is a JSON WebSocket frame:
    //   {"type":"token","data":"partial text"}
    //   {"type":"done","data":"{EditResponse JSON}"}
    //   {"type":"error","data":"reason"}

    private static final String EDIT_SEP = "---EDIT---";

    public Flux<String> streamEditWs(EditRequest request) {
        String parsedJson = safeToJson(request.getParsedData());
        if ("CHAT_ONLY".equals(request.getIntent())) {
            return streamChatOnlyWs(request, parsedJson);
        }
        return streamResumeEditWs(request, parsedJson);
    }

    private Flux<String> streamChatOnlyWs(EditRequest request, String parsedJson) {
        return claudeApiClient.streamChatOnly(parsedJson, request.getMessage(), request.getHistory())
                .map(token -> frame("token", token))
                .concatWith(Mono.fromCallable(() -> {
                    EditResponse unchanged = EditResponse.builder()
                            .updatedParsedData(request.getParsedData())
                            .updatedLatex(request.getLatexSource())
                            .message("").build();
                    return frame("done", objectMapper.writeValueAsString(unchanged));
                }))
                .onErrorResume(e -> Flux.just(frame("error", e.getMessage())));
    }

    private Flux<String> streamResumeEditWs(EditRequest request, String parsedJson) {
        StringBuilder pending    = new StringBuilder();
        StringBuilder jsonAcc    = new StringBuilder();
        AtomicBoolean inJsonPhase = new AtomicBoolean(false);

        return claudeApiClient.streamEditResume(
                        parsedJson, request.getLatexSource(),
                        request.getMessage(), request.getHistory())
                .concatMap(token -> {
                    if (inJsonPhase.get()) {
                        jsonAcc.append(token);
                        return Flux.empty();
                    }
                    pending.append(token);
                    String buf = pending.toString();
                    int sepIdx = buf.indexOf(EDIT_SEP);
                    if (sepIdx >= 0) {
                        String msgPart = buf.substring(0, sepIdx).trim();
                        jsonAcc.append(buf.substring(sepIdx + EDIT_SEP.length()));
                        pending.setLength(0);
                        inJsonPhase.set(true);
                        if (!msgPart.isEmpty()) {
                            return Flux.fromArray(msgPart.split("(?<=\\s)|(?=\\s)"))
                                    .filter(w -> !w.isEmpty())
                                    .map(w -> frame("token", w));
                        }
                        return Flux.empty();
                    }
                    int lastSpace = buf.lastIndexOf(' ');
                    if (lastSpace > 0) {
                        String toEmit = buf.substring(0, lastSpace + 1);
                        pending.delete(0, lastSpace + 1);
                        return Flux.fromArray(toEmit.split("(?<=\\s)|(?=\\s)"))
                                .filter(w -> !w.isEmpty())
                                .map(w -> frame("token", w));
                    }
                    return Flux.empty();
                })
                .concatWith(Flux.defer(() -> {
                    Flux<String> remaining = Flux.empty();
                    if (!inJsonPhase.get() && pending.length() > 0) {
                        String rem = pending.toString().trim();
                        if (!rem.isEmpty()) {
                            remaining = Flux.fromArray(rem.split("(?<=\\s)|(?=\\s)"))
                                    .filter(w -> !w.isEmpty())
                                    .map(w -> frame("token", w));
                        }
                    }
                    return remaining.concatWith(
                            Mono.fromCallable(() -> buildDoneFrame(request, jsonAcc.toString(), inJsonPhase.get())));
                }))
                .onErrorResume(e -> {
                    log.error("Resume edit WS stream error: {}", e.getMessage());
                    return Flux.just(frame("error", e.getMessage()));
                });
    }

    private String buildDoneFrame(EditRequest request, String rawJson, boolean hadEdit) {
        EditResponse editResponse = null;
        if (hadEdit && rawJson != null && !rawJson.isBlank()) {
            try {
                String trimmed = stripMarkdown(rawJson.trim());
                var map = objectMapper.readValue(trimmed, Map.class);
                ParsedResumeData updatedParsed = objectMapper.convertValue(
                        map.get("updated_parsed"), ParsedResumeData.class);
                String updatedLatex = (String) map.get("updated_latex");
                if (updatedParsed != null && updatedLatex != null) {
                    editResponse = EditResponse.builder()
                            .updatedParsedData(updatedParsed)
                            .updatedLatex(updatedLatex)
                            .message("").build();
                } else {
                    log.warn("Claude edit JSON missing fields — falling back to unchanged data");
                }
            } catch (Exception e) {
                log.error("Failed to parse Claude edit JSON (fallback to unchanged): {}", e.getMessage());
            }
        }
        if (editResponse == null) {
            editResponse = EditResponse.builder()
                    .updatedParsedData(request.getParsedData())
                    .updatedLatex(request.getLatexSource())
                    .message("").build();
        }
        try {
            return frame("done", objectMapper.writeValueAsString(editResponse));
        } catch (Exception e) {
            return frame("error", "Failed to serialize response");
        }
    }

    private String frame(String type, String data) {
        try {
            return objectMapper.writeValueAsString(Map.of("type", type, "data", data != null ? data : ""));
        } catch (Exception e) {
            return "{\"type\":\"error\",\"data\":\"serialization_error\"}";
        }
    }

    /** Strip markdown code fences Claude sometimes adds despite instructions */
    private String stripMarkdown(String text) {
        if (text == null) return "{}";
        String t = text.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("```[a-zA-Z]*\\n?", "");
            int end = t.lastIndexOf("```");
            if (end >= 0) t = t.substring(0, end);
        }
        return t.trim();
    }

    private String safeToJson(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (Exception e) { return "{}"; }
    }
}
