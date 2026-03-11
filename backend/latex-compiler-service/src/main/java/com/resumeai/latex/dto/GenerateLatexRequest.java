package com.resumeai.latex.dto;

import com.resumeai.common.dto.ParsedResumeData;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateLatexRequest {
    @NotNull
    private ParsedResumeData parsedData;
    private String template;
}
