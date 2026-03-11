package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateLatexRequest {
    @NotBlank
    private String latexSource;
}
