package com.resumeai.ai.dto;

import com.resumeai.common.dto.ParsedResumeData;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EditResponse {
    private ParsedResumeData updatedParsedData;
    private String updatedLatex;
    private String message;
}
