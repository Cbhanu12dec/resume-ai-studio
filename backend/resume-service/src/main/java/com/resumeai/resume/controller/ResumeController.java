package com.resumeai.resume.controller;

import com.resumeai.common.dto.ApiResponse;
import com.resumeai.resume.dto.*;
import com.resumeai.resume.service.ResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/resumes")
@RequiredArgsConstructor
@Tag(name = "Resume", description = "Resume management endpoints")
public class ResumeController {

    private final ResumeService resumeService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload resume file — returns immediately; poll GET /{id} for status")
    public ResponseEntity<ApiResponse<ResumeDto>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.accepted().body(ApiResponse.ok(resumeService.uploadAndParse(file, userId)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get resume by ID")
    public ResponseEntity<ApiResponse<ResumeDto>> getById(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getById(id, userId)));
    }

    @GetMapping
    @Operation(summary = "List all resumes for current user")
    public ResponseEntity<ApiResponse<List<ResumeDto>>> list(
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.listByUser(userId)));
    }

    @GetMapping("/{id}/latex")
    @Operation(summary = "Get LaTeX source")
    public ResponseEntity<ApiResponse<LatexDto>> getLatex(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getLatex(id, userId)));
    }

    @PutMapping("/{id}/latex")
    @Operation(summary = "Update LaTeX source directly")
    public ResponseEntity<ApiResponse<LatexDto>> updateLatex(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateLatexRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.updateLatex(id, request, userId)));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Download compiled PDF")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        byte[] pdf = resumeService.downloadPdf(id, userId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header("Content-Disposition", "attachment; filename=\"resume.pdf\"")
                .body(pdf);
    }

    @PostMapping("/{id}/chat")
    @Operation(summary = "Send chat edit request")
    public ResponseEntity<ApiResponse<ChatEditResponse>> chat(
            @PathVariable UUID id,
            @RequestBody @Valid ChatEditRequest request,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.chatEdit(id, request, userId)));
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "List version history")
    public ResponseEntity<ApiResponse<List<VersionDto>>> versions(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.getVersions(id, userId)));
    }

    @PostMapping("/{id}/restore/{versionId}")
    @Operation(summary = "Restore a version")
    public ResponseEntity<ApiResponse<ResumeDto>> restore(
            @PathVariable UUID id,
            @PathVariable UUID versionId,
            @RequestHeader(value = "X-User-Id", defaultValue = "dev-user") String userId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeService.restoreVersion(id, versionId, userId)));
    }
}
