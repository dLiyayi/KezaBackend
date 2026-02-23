package com.keza.campaign.adapter.in.web;

import com.keza.campaign.application.dto.CampaignQuestionAnswerRequest;
import com.keza.campaign.application.dto.CampaignQuestionRequest;
import com.keza.campaign.application.dto.CampaignQuestionResponse;
import com.keza.campaign.application.usecase.CampaignQuestionUseCase;
import com.keza.common.dto.ApiResponse;
import com.keza.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/campaigns/{campaignId}/questions")
@RequiredArgsConstructor
public class CampaignQuestionController {

    private final CampaignQuestionUseCase campaignQuestionUseCase;

    @PostMapping
    public ResponseEntity<ApiResponse<CampaignQuestionResponse>> askQuestion(
            @PathVariable UUID campaignId,
            @Valid @RequestBody CampaignQuestionRequest request,
            Authentication authentication) {
        UUID askerId = UUID.fromString(authentication.getName());
        CampaignQuestionResponse response = campaignQuestionUseCase.askQuestion(campaignId, askerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Question submitted successfully"));
    }

    @PutMapping("/{questionId}/answer")
    @PreAuthorize("hasRole('ISSUER')")
    public ResponseEntity<ApiResponse<CampaignQuestionResponse>> answerQuestion(
            @PathVariable UUID campaignId,
            @PathVariable UUID questionId,
            @Valid @RequestBody CampaignQuestionAnswerRequest request,
            Authentication authentication) {
        UUID answererId = UUID.fromString(authentication.getName());
        CampaignQuestionResponse response = campaignQuestionUseCase.answerQuestion(
                campaignId, questionId, answererId, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Question answered successfully"));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CampaignQuestionResponse>>> getQuestions(
            @PathVariable UUID campaignId,
            @RequestParam(defaultValue = "false") boolean answeredOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        size = Math.min(size, 100);
        Page<CampaignQuestionResponse> questions = campaignQuestionUseCase.getQuestions(
                campaignId, answeredOnly, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(ApiResponse.success(PagedResponse.from(questions)));
    }
}
