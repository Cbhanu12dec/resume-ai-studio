package com.resumeai.latex.controller;

import com.resumeai.common.dto.ApiResponse;
import com.resumeai.common.dto.ParsedResumeData;
import com.resumeai.latex.dto.GenerateLatexRequest;
import com.resumeai.latex.dto.GenerateLatexResponse;
import com.resumeai.latex.service.LatexGeneratorService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/latex")
@RequiredArgsConstructor
@Tag(name = "LaTeX", description = "LaTeX generation and compilation")
public class LatexController {

    private final LatexGeneratorService latexService;

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<GenerateLatexResponse>> generate(
            @RequestBody @Valid GenerateLatexRequest request) {
        String latex = latexService.generate(request.getParsedData(), request.getTemplate());
        return ResponseEntity.ok(ApiResponse.ok(GenerateLatexResponse.builder().latex(latex).build()));
    }

    @PostMapping("/compile")
    public ResponseEntity<byte[]> compile(@RequestBody String latexSource) {
        byte[] pdf = latexService.compile(latexSource);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
