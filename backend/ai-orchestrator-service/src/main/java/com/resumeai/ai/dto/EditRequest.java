package com.resumeai.ai.dto;

import com.resumeai.common.dto.ParsedResumeData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class EditRequest {
    @NotNull
    private ParsedResumeData parsedData;
    @NotBlank
    private String latexSource;
    @NotBlank
    private String message;
    private List<Map<String, String>> history;
}
