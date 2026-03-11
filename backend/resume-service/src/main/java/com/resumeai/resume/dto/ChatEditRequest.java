package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class ChatEditRequest {
    @NotBlank
    private String message;
    private List<ChatMessage> history;

    @Data
    public static class ChatMessage {
        private String role; // "user" | "assistant"
        private String content;
    }
}
