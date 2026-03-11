package com.resumeai.resume.controller;

import com.resumeai.common.dto.ApiResponse;
import com.resumeai.resume.dto.MatchScoreRequest;
import com.resumeai.resume.service.MatchService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/match")
@RequiredArgsConstructor
@Tag(name = "Job Match", description = "Resume vs Job Description scoring")
public class MatchController {

    private final MatchService matchService;

    @PostMapping("/score")
    public ResponseEntity<ApiResponse<?>> score(
            @RequestBody @Valid MatchScoreRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(matchService.score(request, userId)));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<?>>> history(
            @RequestParam UUID resumeId,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(matchService.history(resumeId, userId)));
    }
}
