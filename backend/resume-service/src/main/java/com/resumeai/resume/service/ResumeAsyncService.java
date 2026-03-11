package com.resumeai.resume.service;

import com.resumeai.resume.client.AiOrchestratorClient;
import com.resumeai.resume.client.LatexCompilerClient;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import com.resumeai.resume.entity.ResumeVersion;
import com.resumeai.resume.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ResumeAsyncService {

    private final ResumeRepository resumeRepository;
    private final S3StorageService s3StorageService;
    private final AiOrchestratorClient aiClient;
    private final LatexCompilerClient latexClient;

    @Async("resumeProcessingExecutor")
    @Transactional
    public void processResume(UUID resumeId, byte[] fileBytes, String contentType, String userId) {
        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new RuntimeException("Resume not found: " + resumeId));
        try {
            log.info("Async processing resume id={}", resumeId);

            // Parse with Claude AI
            var parseResult = aiClient.parseResume(fileBytes, contentType);
            resume.setParsedData(parseResult.getParsedData());
            resume.setStatus(ResumeStatus.GENERATING_LATEX);
            resumeRepository.save(resume);

            // Generate LaTeX
            String latex = latexClient.generateLatex(parseResult.getParsedData());
            String latexKey = s3StorageService.uploadLatex(latex, userId, resume.getId());
            resume.setLatexSource(latex);
            resume.setS3LatexKey(latexKey);
            resume.setStatus(ResumeStatus.READY);

            // Save initial version
            ResumeVersion version = ResumeVersion.builder()
                    .resume(resume)
                    .versionNumber(1)
                    .latexSource(latex)
                    .changeDescription("Initial parse")
                    .build();
            resume.getVersions().add(version);
            resumeRepository.save(resume);

            log.info("Resume id={} processing complete", resumeId);
        } catch (Exception e) {
            log.error("Failed to process resume id={}", resumeId, e);
            resume.setStatus(ResumeStatus.ERROR);
            resumeRepository.save(resume);
        }
    }
}
