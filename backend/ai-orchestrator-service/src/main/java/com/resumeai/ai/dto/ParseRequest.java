package com.resumeai.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParseRequest {
    @NotBlank
    private String fileBase64;
    @NotBlank
    private String mediaType;
}
