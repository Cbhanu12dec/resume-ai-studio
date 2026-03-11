package com.resumeai.ai.controller;

import com.resumeai.ai.dto.*;
import com.resumeai.ai.service.AiOrchestratorService;
import com.resumeai.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "AI Orchestrator", description = "Claude AI integration endpoints")
public class AiController {

    private final AiOrchestratorService aiService;

    @PostMapping("/parse")
    public Mono<ApiResponse<ParseResponse>> parseResume(
            @RequestBody @Valid ParseRequest request) {
        return aiService.parseResume(request).map(ApiResponse::ok);
    }

    @PostMapping("/edit")
    public Mono<ApiResponse<EditResponse>> editResume(
            @RequestBody @Valid EditRequest request) {
        return aiService.editResume(request).map(ApiResponse::ok);
    }

    @PostMapping("/match")
    public Mono<ApiResponse<MatchResponse>> matchResume(
            @RequestBody @Valid MatchRequest request) {
        return aiService.matchResume(request).map(ApiResponse::ok);
    }
}
