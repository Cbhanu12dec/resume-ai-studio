package com.resumeai.resume.entity;

import com.resumeai.common.dto.ParsedResumeData;
import io.hypersistence.utils.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "resumes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String userId;

    @Column
    private String originalFileName;

    @Column
    private String s3OriginalKey;

    @Column
    private String s3LatexKey;

    @Column
    private String s3PdfKey;

    @Type(JsonType.class)
    @Column(columnDefinition = "jsonb")
    private ParsedResumeData parsedData;

    @Column(columnDefinition = "TEXT")
    private String latexSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.PENDING;

    @Column(nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ResumeVersion> versions = new ArrayList<>();

    @OneToMany(mappedBy = "resume", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MatchResult> matchResults = new ArrayList<>();

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
