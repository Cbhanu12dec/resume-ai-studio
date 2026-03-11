package com.resumeai.ai.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ClaudeContent {
    private String type;
    private String text;
    private DocumentSource source;

    public static ClaudeContent text(String text) {
        return ClaudeContent.builder().type("text").text(text).build();
    }

    /** For image files: JPEG, PNG, GIF, WebP */
    public static ClaudeContent image(String mediaType, String base64Data) {
        return ClaudeContent.builder()
                .type("image")
                .source(DocumentSource.builder()
                        .type("base64")
                        .mediaType(mediaType)
                        .data(base64Data)
                        .build())
                .build();
    }

    /** For PDF documents — uses Claude's native document support */
    public static ClaudeContent document(String mediaType, String base64Data) {
        return ClaudeContent.builder()
                .type("document")
                .source(DocumentSource.builder()
                        .type("base64")
                        .mediaType(mediaType)
                        .data(base64Data)
                        .build())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DocumentSource {
        private String type;
        @JsonProperty("media_type")
        private String mediaType;
        private String data;
    }
}
