package com.resumeai.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.ai.client.ClaudeApiClient;
import com.resumeai.ai.dto.*;
import com.resumeai.common.dto.ParsedResumeData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.List;
import java.util.Map;

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
