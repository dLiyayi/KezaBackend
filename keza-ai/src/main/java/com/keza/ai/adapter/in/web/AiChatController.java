package com.keza.ai.adapter.in.web;

import com.keza.ai.application.dto.ChatRequest;
import com.keza.ai.application.dto.ChatResponse;
import com.keza.ai.application.dto.ChatSessionResponse;
import com.keza.ai.application.usecase.ChatUseCase;
import com.keza.common.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final ChatUseCase chatUseCase;

    /**
     * Creates a new chat session for the authenticated user.
     */
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<ChatSessionResponse>> createSession(
            Authentication authentication,
            @RequestParam(value = "language", defaultValue = "en") String language) {
        UUID userId = (UUID) authentication.getPrincipal();
        ChatSessionResponse session = chatUseCase.createSession(userId, language);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(session, "Chat session created"));
    }

    /**
     * Sends a message in an existing chat session and receives an AI response.
     */
    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<ChatResponse>> sendMessage(
            Authentication authentication,
            @PathVariable("id") UUID sessionId,
            @Valid @RequestBody ChatRequest request) {
        UUID userId = (UUID) authentication.getPrincipal();
        ChatResponse response = chatUseCase.chat(userId, sessionId, request.getMessage());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Lists all active chat sessions for the authenticated user.
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<ChatSessionResponse>>> listSessions(Authentication authentication) {
        UUID userId = (UUID) authentication.getPrincipal();
        List<ChatSessionResponse> sessions = chatUseCase.getSessions(userId);
        return ResponseEntity.ok(ApiResponse.success(sessions));
    }

    /**
     * Retrieves all messages in a chat session.
     */
    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<ApiResponse<List<ChatResponse>>> getMessages(@PathVariable("id") UUID sessionId) {
        List<ChatResponse> messages = chatUseCase.getSessionMessages(sessionId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Deletes (deactivates) a chat session.
     */
    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSession(@PathVariable("id") UUID sessionId) {
        chatUseCase.deleteSession(sessionId);
        return ResponseEntity.ok(ApiResponse.success(null, "Chat session deleted"));
    }
}
