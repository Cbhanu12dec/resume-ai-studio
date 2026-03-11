package com.resumeai.ai.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClaudeMessage {
    private String role;
    private Object content; // String or List<ClaudeContent>

    public static ClaudeMessage user(List<ClaudeContent> content) {
        return new ClaudeMessage("user", content);
    }

    public static ClaudeMessage assistant(String text) {
        return new ClaudeMessage("assistant", text);
    }
}
