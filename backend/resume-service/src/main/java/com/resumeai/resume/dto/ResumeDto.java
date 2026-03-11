package com.resumeai.resume.dto;

import com.resumeai.common.dto.ParsedResumeData;
import com.resumeai.resume.entity.ResumeStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class ResumeDto {
    private UUID id;
    private String userId;
    private String originalFileName;
    private ResumeStatus status;
    private ParsedResumeData parsedData;
    private String latexSource;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
