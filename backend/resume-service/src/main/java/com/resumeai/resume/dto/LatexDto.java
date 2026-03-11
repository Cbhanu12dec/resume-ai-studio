package com.resumeai.resume.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class LatexDto {
    private UUID resumeId;
    private String latexSource;
    private Integer versionNumber;
}
