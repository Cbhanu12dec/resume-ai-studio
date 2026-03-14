package com.resumeai.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaudeRequest {
    private String model;
    @JsonProperty("max_tokens")
    private int maxTokens;
    private String system;
    private List<ClaudeMessage> messages;
    /** Set to true to enable server-sent event streaming from Claude */
    private Boolean stream;
}
