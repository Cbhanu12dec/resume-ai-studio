package com.resumeai.ai.dto;

import com.resumeai.common.dto.ParsedResumeData;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class MatchRequest {
    @NotNull
    private ParsedResumeData resumeData;
    @NotBlank
    private String jobDescription;
}
