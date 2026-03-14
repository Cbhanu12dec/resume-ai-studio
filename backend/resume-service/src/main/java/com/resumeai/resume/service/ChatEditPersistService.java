package com.resumeai.resume.service;

import com.resumeai.resume.dto.ChatEditResponse;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeVersion;
import com.resumeai.resume.repository.ResumeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatEditPersistService {

    private final ResumeRepository resumeRepository;

    /**
     * Saves the AI-generated resume edit to the database.
     * Throws on failure so callers can decide whether to surface the error or
     * still send the AI result to the frontend.
     */
    @Transactional
    public void persistEdit(UUID resumeId, ChatEditResponse result) {
        if (result.getUpdatedParsedData() == null || result.getUpdatedLatex() == null) {
            log.warn("Edit response missing fields for resume {} — skipping DB save", resumeId);
            return;
        }

        Resume resume = resumeRepository.findById(resumeId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found: " + resumeId));

        int newVersion = resume.getVersions().size() + 1;
        ResumeVersion version = ResumeVersion.builder()
                .resume(resume)
                .versionNumber(newVersion)
                .latexSource(result.getUpdatedLatex())
                .changeDescription("Chat edit (streamed)")
                .build();
        resume.getVersions().add(version);
        resume.setParsedData(result.getUpdatedParsedData());
        resume.setLatexSource(result.getUpdatedLatex());
        resume.setS3PdfKey(null); // invalidate cached PDF
        resumeRepository.save(resume);

        log.info("Persisted chat edit for resume {} as version {}", resumeId, newVersion);
    }
}
