package com.resumeai.ai.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ClaudeRequest {
    private String model;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private String system;
    private List<ClaudeMessage> messages;
}
