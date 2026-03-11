package com.resumeai.resume.repository;

import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.entity.ResumeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByUserIdOrderByCreatedAtDesc(String userId);
    Optional<Resume> findByIdAndUserId(UUID id, String userId);
    List<Resume> findByStatus(ResumeStatus status);
}
