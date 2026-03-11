package com.resumeai.latex.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GenerateLatexResponse {
    private String latex;
}
