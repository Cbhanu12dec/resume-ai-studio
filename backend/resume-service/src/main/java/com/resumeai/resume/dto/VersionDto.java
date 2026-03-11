package com.resumeai.resume.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
public class VersionDto {
    private UUID id;
    private Integer versionNumber;
    private String changeDescription;
    private LocalDateTime createdAt;
}
