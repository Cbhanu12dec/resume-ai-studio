package com.resumeai.ai.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class MatchResponse {
    private Integer score;
    private Map<String, Integer> breakdown;
    private List<String> matchingKeywords;
    private List<String> missingKeywords;
    private List<String> suggestions;
}
