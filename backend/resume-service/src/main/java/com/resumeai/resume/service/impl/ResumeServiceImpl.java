package com.resumeai.resume.service.impl;

import com.resumeai.resume.client.AiOrchestratorClient;
import com.resumeai.resume.client.LatexCompilerClient;
import com.resumeai.resume.dto.*;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.entity.ResumeVersion;
import com.resumeai.resume.mapper.ResumeMapper;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.ResumeAsyncService;
import com.resumeai.resume.service.ResumeService;
import com.resumeai.resume.service.S3StorageService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final S3StorageService s3StorageService;
    private final AiOrchestratorClient aiClient;
    private final LatexCompilerClient latexClient;
    private final ResumeMapper resumeMapper;
    private final ResumeAsyncService resumeAsyncService;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ResumeDto uploadAndParse(MultipartFile file, String userId) {
        log.info("Uploading resume for user={} file={}", userId, file.getOriginalFilename());

        // Upload raw file to S3
        String s3Key = s3StorageService.uploadResume(file, userId);

        // Create DB record — no outer transaction so Spring Data commits immediately
        Resume resume = Resume.builder()
                .userId(userId)
                .originalFileName(file.getOriginalFilename())
                .s3OriginalKey(s3Key)
                .status(ResumeStatus.PARSING)
                .build();
        resume = resumeRepository.save(resume);

        // Kick off async processing — record is now committed and visible to the worker thread
        try {
            byte[] fileBytes = file.getBytes();
            resumeAsyncService.processResume(resume.getId(), fileBytes, file.getContentType(), userId);
        } catch (Exception e) {
            log.error("Failed to submit async processing for resume id={}", resume.getId(), e);
            resume.setStatus(ResumeStatus.ERROR);
            resumeRepository.save(resume);
        }

        // Return immediately — frontend will poll for status
        return resumeMapper.toDto(resume);
    }

    @Override
    @Transactional(readOnly = true)
    public ResumeDto getById(UUID id, String userId) {
        return resumeMapper.toDto(findByIdAndUser(id, userId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ResumeDto> listByUser(String userId) {
        return resumeRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(resumeMapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LatexDto getLatex(UUID id, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        return LatexDto.builder()
                .resumeId(id)
                .latexSource(resume.getLatexSource())
                .versionNumber(resume.getVersions().size())
                .build();
    }

    @Override
    public LatexDto updateLatex(UUID id, UpdateLatexRequest request, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        int newVersion = resume.getVersions().size() + 1;
        saveVersion(resume, newVersion, "Manual edit", request.getLatexSource());
        resume.setLatexSource(request.getLatexSource());
        resume = resumeRepository.save(resume);
        return LatexDto.builder()
                .resumeId(id)
                .latexSource(resume.getLatexSource())
                .versionNumber(newVersion)
                .build();
    }

    @Override
    public byte[] downloadPdf(UUID id, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        if (resume.getS3PdfKey() != null) {
            return s3StorageService.downloadFile(resume.getS3PdfKey());
        }
        // Trigger compilation
        byte[] pdf = latexClient.compilePdf(resume.getLatexSource());
        String pdfKey = s3StorageService.uploadPdf(pdf, userId, id);
        resume.setS3PdfKey(pdfKey);
        resumeRepository.save(resume);
        return pdf;
    }

    @Override
    public ChatEditResponse chatEdit(UUID id, ChatEditRequest request, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        var editResult = aiClient.editResume(resume.getParsedData(), resume.getLatexSource(), request);
        int newVersion = resume.getVersions().size() + 1;
        saveVersion(resume, newVersion, "Chat edit: " + request.getMessage(), editResult.getUpdatedLatex());
        resume.setParsedData(editResult.getUpdatedParsedData());
        resume.setLatexSource(editResult.getUpdatedLatex());
        resume.setS3PdfKey(null); // invalidate cached PDF
        resumeRepository.save(resume);
        return editResult;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VersionDto> getVersions(UUID id, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        return resume.getVersions().stream()
                .map(v -> {
                    VersionDto dto = new VersionDto();
                    dto.setId(v.getId());
                    dto.setVersionNumber(v.getVersionNumber());
                    dto.setChangeDescription(v.getChangeDescription());
                    dto.setCreatedAt(v.getCreatedAt());
                    return dto;
                }).toList();
    }

    @Override
    public ResumeDto restoreVersion(UUID id, UUID versionId, String userId) {
        Resume resume = findByIdAndUser(id, userId);
        ResumeVersion version = resume.getVersions().stream()
                .filter(v -> v.getId().equals(versionId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Version not found: " + versionId));
        resume.setLatexSource(version.getLatexSource());
        resume.setS3PdfKey(null);
        int newVersion = resume.getVersions().size() + 1;
        saveVersion(resume, newVersion, "Restored to v" + version.getVersionNumber(), version.getLatexSource());
        return resumeMapper.toDto(resumeRepository.save(resume));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Resume findByIdAndUser(UUID id, String userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found: " + id));
    }

    private void saveVersion(Resume resume, int versionNumber, String description, String latex) {
        ResumeVersion version = ResumeVersion.builder()
                .resume(resume)
                .versionNumber(versionNumber)
                .latexSource(latex)
                .changeDescription(description)
                .build();
        resume.getVersions().add(version);
    }
}
