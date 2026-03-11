package com.resumeai.resume.dto;

import com.resumeai.common.dto.ParsedResumeData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatEditResponse {
    private String message;
    private ParsedResumeData updatedParsedData;
    private String updatedLatex;
}
