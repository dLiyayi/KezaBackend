package com.keza.ai.application.usecase;

import com.keza.ai.application.dto.ChatResponse;
import com.keza.ai.application.dto.ChatSessionResponse;
import com.keza.ai.domain.model.ChatMessage;
import com.keza.ai.domain.model.ChatSession;
import com.keza.ai.domain.port.out.ChatMessageRepository;
import com.keza.ai.domain.port.out.ChatSessionRepository;
import com.keza.ai.domain.service.AiChatService;
import com.keza.ai.domain.service.StubAiChatService;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatUseCase {

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;

    // Only one of these will be injected depending on keza.ai.enabled property
    @Autowired(required = false)
    private AiChatService aiChatService;

    @Autowired(required = false)
    private StubAiChatService stubAiChatService;

    private static final int MAX_SESSIONS_PER_USER = 50;

    /**
     * Creates a new chat session for the user.
     */
    @Transactional
    public ChatSessionResponse createSession(UUID userId, String language) {
        long sessionCount = chatSessionRepository.countByUserId(userId);
        if (sessionCount >= MAX_SESSIONS_PER_USER) {
            throw new BusinessRuleException("MAX_SESSIONS_REACHED",
                    "Maximum number of chat sessions (" + MAX_SESSIONS_PER_USER + ") reached. Please delete old sessions.");
        }

        String lang = (language != null && !language.isBlank()) ? language.trim().toLowerCase() : "en";

        ChatSession session = ChatSession.builder()
                .userId(userId)
                .title("New conversation")
                .language(lang)
                .active(true)
                .messageCount(0)
                .build();

        session = chatSessionRepository.save(session);
        log.info("Created chat session {} for user {}", session.getId(), userId);

        return mapToSessionResponse(session);
    }

    /**
     * Sends a message in an existing chat session and returns the AI response.
     */
    @Transactional
    public ChatResponse chat(UUID userId, UUID sessionId, String message) {
        ChatSession session = chatSessionRepository.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));

        if (!session.isActive()) {
            throw new BusinessRuleException("SESSION_INACTIVE", "This chat session is no longer active.");
        }

        // Save user message
        ChatMessage userMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("user")
                .content(message)
                .build();
        chatMessageRepository.save(userMessage);

        // Get AI response (real or stub)
        String aiResponse = getChatResponse(sessionId, message, session.getLanguage());

        // Save assistant message
        ChatMessage assistantMessage = ChatMessage.builder()
                .sessionId(sessionId)
                .role("assistant")
                .content(aiResponse)
                .build();
        chatMessageRepository.save(assistantMessage);

        // Update session metadata
        session.setMessageCount(session.getMessageCount() + 2);

        // Auto-generate title from first user message
        if (session.getMessageCount() <= 2 && "New conversation".equals(session.getTitle())) {
            String title = message.length() > 50 ? message.substring(0, 50) + "..." : message;
            session.setTitle(title);
        }

        chatSessionRepository.save(session);

        return ChatResponse.builder()
                .sessionId(sessionId)
                .message(aiResponse)
                .role("assistant")
                .build();
    }

    /**
     * Returns all active chat sessions for a user.
     */
    @Transactional(readOnly = true)
    public List<ChatSessionResponse> getSessions(UUID userId) {
        return chatSessionRepository.findByUserIdAndActiveTrueOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToSessionResponse)
                .toList();
    }

    /**
     * Returns all messages in a chat session, ordered chronologically.
     */
    @Transactional(readOnly = true)
    public List<ChatResponse> getSessionMessages(UUID sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId)
                .stream()
                .map(msg -> ChatResponse.builder()
                        .sessionId(msg.getSessionId())
                        .message(msg.getContent())
                        .role(msg.getRole())
                        .build())
                .toList();
    }

    /**
     * Soft-deletes a chat session by marking it inactive.
     */
    @Transactional
    public void deleteSession(UUID sessionId) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("ChatSession", sessionId));

        session.setActive(false);
        chatSessionRepository.save(session);
        log.info("Deactivated chat session {}", sessionId);
    }

    // ---- Private helpers ----

    private String getChatResponse(UUID sessionId, String message, String language) {
        if (aiChatService != null) {
            return aiChatService.chat(sessionId, message, language);
        } else if (stubAiChatService != null) {
            return stubAiChatService.chat(sessionId, message, language);
        } else {
            return "AI service is not available. Please try again later.";
        }
    }

    private ChatSessionResponse mapToSessionResponse(ChatSession session) {
        return ChatSessionResponse.builder()
                .id(session.getId())
                .title(session.getTitle())
                .language(session.getLanguage())
                .messageCount(session.getMessageCount())
                .createdAt(session.getCreatedAt())
                .build();
    }
}
