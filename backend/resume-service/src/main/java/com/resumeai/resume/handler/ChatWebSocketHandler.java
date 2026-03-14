package com.resumeai.resume.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumeai.resume.client.AiOrchestratorClient;
import com.resumeai.resume.dto.ChatEditResponse;
import com.resumeai.resume.entity.Resume;
import com.resumeai.resume.repository.ResumeRepository;
import com.resumeai.resume.service.ChatEditPersistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Servlet-based WebSocket handler for browser ↔ resume-service streaming chat.
 *
 * URL:    ws://localhost:8081/ws/chat
 * Client → server (first frame):
 *   {"resumeId":"...", "message":"...", "history":[...]}
 * Client → server (cancel):
 *   {"type":"cancel"}
 * Server → client:
 *   {"type":"token","data":"partial text"}   ← chat-only streaming
 *   {"type":"done","data":"{ChatEditResponse JSON}"}
 *   {"type":"error","data":"reason"}
 *
 * Intent routing:
 *   EDIT_RESUME → non-streaming HTTP to ai-orchestrator (reliable, same path as pre-WS migration)
 *   CHAT_ONLY   → streaming WS to ai-orchestrator (shows live tokens)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final AiOrchestratorClient aiClient;
    private final ChatEditPersistService persistService;
    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    private final Map<String, Disposable> activeStreams = new ConcurrentHashMap<>();

    @Override
    @SuppressWarnings("unchecked")
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<?, ?> payload = objectMapper.readValue(message.getPayload(), Map.class);

        if ("cancel".equals(payload.get("type"))) {
            Disposable sub = activeStreams.remove(session.getId());
            if (sub != null && !sub.isDisposed()) sub.dispose();
            return;
        }

        String resumeIdStr = (String) payload.get("resumeId");
        String msg = (String) payload.get("message");
        List<Map<String, String>> history = (List<Map<String, String>>) payload.get("history");

        if (resumeIdStr == null || msg == null || msg.isBlank()) {
            sendFrame(session, "error", "Missing resumeId or message");
            return;
        }

        UUID resumeId;
        try {
            resumeId = UUID.fromString(resumeIdStr);
        } catch (IllegalArgumentException e) {
            sendFrame(session, "error", "Invalid resumeId");
            return;
        }

        Resume resume = resumeRepository.findById(resumeId).orElse(null);
        if (resume == null) {
            sendFrame(session, "error", "Resume not found: " + resumeId);
            return;
        }

        String intent = detectIntent(msg);
        log.info("Chat WS session={} resumeId={} intent={}", session.getId(), resumeId, intent);

        Disposable sub;
        if ("CHAT_ONLY".equals(intent)) {
            sub = handleChatOnly(session, resume, msg, history);
        } else {
            sub = handleEdit(session, resume, resumeId, msg, history);
        }
        activeStreams.put(session.getId(), sub);
    }

    // ─── EDIT_RESUME: reliable non-streaming HTTP (same path as pre-WS migration) ───

    private Disposable handleEdit(WebSocketSession session, Resume resume, UUID resumeId,
                                  String msg, List<Map<String, String>> history) {
        return Mono.fromCallable(() ->
                        aiClient.editResumeRaw(resume.getParsedData(), resume.getLatexSource(), msg, history))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                        result -> {
                            // Persist to DB — best-effort, don't block UI update on failure
                            if (result.getUpdatedParsedData() != null && result.getUpdatedLatex() != null) {
                                try {
                                    persistService.persistEdit(resumeId, result);
                                } catch (Exception e) {
                                    log.error("DB save failed for resume {} (UI will still update): {}",
                                            resumeId, e.getMessage());
                                }
                            }
                            try {
                                sendFrame(session, "done", objectMapper.writeValueAsString(result));
                            } catch (Exception e) {
                                log.error("Failed to serialize edit result session={}", session.getId(), e);
                                sendFrame(session, "error", "Serialization error");
                            }
                        },
                        error -> {
                            log.error("Edit HTTP call failed session={}: {}", session.getId(), error.getMessage());
                            sendFrame(session, "error",
                                    error.getMessage() != null ? error.getMessage() : "Edit failed");
                        }
                );
    }

    // ─── CHAT_ONLY: streaming WS to ai-orchestrator (live tokens) ─────────────

    private Disposable handleChatOnly(WebSocketSession session, Resume resume,
                                      String msg, List<Map<String, String>> history) {
        return aiClient.streamEditResume(resume.getParsedData(), resume.getLatexSource(),
                        msg, history, "CHAT_ONLY")
                .publishOn(Schedulers.boundedElastic())
                .doOnNext(frame -> {
                    try {
                        if ("done".equals(frame.type())) {
                            // CHAT_ONLY done: parse and send to frontend (no DB save needed for questions)
                            ChatEditResponse aiResult = objectMapper.readValue(
                                    frame.data(), ChatEditResponse.class);
                            sendFrame(session, "done", objectMapper.writeValueAsString(aiResult));
                        } else {
                            sendFrame(session, frame.type(), frame.data());
                        }
                    } catch (Exception e) {
                        log.error("Error handling chat-only frame session={}", session.getId(), e);
                        sendFrame(session, "error", e.getMessage());
                    }
                })
                .doOnError(e -> {
                    log.error("Chat-only stream error session={}: {}", session.getId(), e.getMessage());
                    sendFrame(session, "error", e.getMessage() != null ? e.getMessage() : "Stream failed");
                })
                .doFinally(sig -> {
                    activeStreams.remove(session.getId());
                    log.debug("Chat stream done session={} signal={}", session.getId(), sig);
                })
                .subscribe();
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Disposable sub = activeStreams.remove(session.getId());
        if (sub != null && !sub.isDisposed()) sub.dispose();
        log.debug("Chat WS closed session={} status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Chat WS transport error session={}: {}", session.getId(), exception.getMessage());
        Disposable sub = activeStreams.remove(session.getId());
        if (sub != null && !sub.isDisposed()) sub.dispose();
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private void sendFrame(WebSocketSession session, String type, String data) {
        try {
            if (session.isOpen()) {
                Map<String, String> frame = Map.of("type", type, "data", data != null ? data : "");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(frame)));
            }
        } catch (Exception e) {
            log.warn("Failed to send frame type={} session={}: {}", type, session.getId(), e.getMessage());
        }
    }

    private static final Pattern QUESTION_ONLY_PATTERN = Pattern.compile(
            "^(what|how|why|when|where|who|which|is |are |was |were |do |does |did |" +
            "can you tell|tell me about|explain|show me what|list my|what are my|how is my|" +
            "what does|how does)\\b",
            Pattern.CASE_INSENSITIVE
    );

    private String detectIntent(String message) {
        if (message == null || message.isBlank()) return "CHAT_ONLY";
        return QUESTION_ONLY_PATTERN.matcher(message.trim()).find() ? "CHAT_ONLY" : "EDIT_RESUME";
    }
}
