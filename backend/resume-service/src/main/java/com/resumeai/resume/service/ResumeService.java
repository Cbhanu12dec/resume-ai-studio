package com.resumeai.resume.service;

import com.resumeai.resume.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {
    ResumeDto uploadAndParse(MultipartFile file, String userId);
    ResumeDto getById(UUID id, String userId);
    List<ResumeDto> listByUser(String userId);
    LatexDto getLatex(UUID id, String userId);
    LatexDto updateLatex(UUID id, UpdateLatexRequest request, String userId);
    byte[] downloadPdf(UUID id, String userId);
    ChatEditResponse chatEdit(UUID id, ChatEditRequest request, String userId);
    List<VersionDto> getVersions(UUID id, String userId);
    ResumeDto restoreVersion(UUID id, UUID versionId, String userId);
}
