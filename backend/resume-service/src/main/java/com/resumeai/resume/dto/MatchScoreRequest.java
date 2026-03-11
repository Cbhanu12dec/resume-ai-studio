package com.resumeai.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class MatchScoreRequest {
    @NotNull
    private UUID resumeId;
    @NotBlank
    private String jobDescription;
}
