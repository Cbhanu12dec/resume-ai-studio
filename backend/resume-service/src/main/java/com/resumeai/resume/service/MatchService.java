package com.resumeai.resume.service;

import com.resumeai.resume.client.AiOrchestratorClient;
import com.resumeai.resume.dto.MatchScoreRequest;
import com.resumeai.resume.entity.MatchResult;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.repository.MatchResultRepository;
import com.resumeai.resume.repository.ResumeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchService {

    private final ResumeRepository resumeRepository;
    private final MatchResultRepository matchResultRepository;
    private final WebClient.Builder webClientBuilder;

    private String aiServiceUrl = "http://localhost:8082";

    public Object score(MatchScoreRequest request, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(request.getResumeId(), userId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found"));

        var body = Map.of(
                "resumeData", resume.getParsedData(),
                "jobDescription", request.getJobDescription()
        );

        var result = webClientBuilder.baseUrl(aiServiceUrl).build()
                .post()
                .uri("/api/v1/ai/match")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        var data = (Map<?, ?>) ((Map<?, ?>) result).get("data");

        // Persist
        MatchResult mr = MatchResult.builder()
                .resume(resume)
                .jobDescription(request.getJobDescription())
                .overallScore((Integer) data.get("score"))
                .matchingKeywords((List<String>) data.get("matchingKeywords"))
                .missingKeywords((List<String>) data.get("missingKeywords"))
                .suggestions((List<String>) data.get("suggestions"))
                .breakdown((Map<String, Integer>) data.get("breakdown"))
                .build();
        matchResultRepository.save(mr);

        return data;
    }

    public List<MatchResult> history(UUID resumeId, String userId) {
        Resume resume = resumeRepository.findByIdAndUserId(resumeId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Resume not found"));
        return resume.getMatchResults();
    }
}
