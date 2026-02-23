package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.CampaignQuestionAnswerRequest;
import com.keza.campaign.application.dto.CampaignQuestionRequest;
import com.keza.campaign.application.dto.CampaignQuestionResponse;
import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.model.CampaignQuestion;
import com.keza.campaign.domain.port.out.CampaignQuestionRepository;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CampaignQuestionUseCase {

    private final CampaignQuestionRepository campaignQuestionRepository;
    private final CampaignRepository campaignRepository;

    @Transactional
    public CampaignQuestionResponse askQuestion(UUID campaignId, UUID askerId, CampaignQuestionRequest request) {
        campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        CampaignQuestion question = CampaignQuestion.builder()
                .campaignId(campaignId)
                .askerId(askerId)
                .question(request.getQuestion())
                .build();

        question = campaignQuestionRepository.save(question);
        log.info("Question {} asked on campaign {} by user {}", question.getId(), campaignId, askerId);
        return mapToResponse(question);
    }

    @Transactional
    public CampaignQuestionResponse answerQuestion(UUID campaignId, UUID questionId, UUID answererId,
                                                    CampaignQuestionAnswerRequest request) {
        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        if (!campaign.getIssuerId().equals(answererId)) {
            throw new BusinessRuleException("FORBIDDEN", "Only the campaign issuer can answer questions");
        }

        CampaignQuestion question = campaignQuestionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question", questionId));

        if (!question.getCampaignId().equals(campaignId)) {
            throw new ResourceNotFoundException("Question", questionId);
        }

        if (question.getAnswer() != null) {
            throw new BusinessRuleException("ALREADY_ANSWERED", "This question has already been answered");
        }

        question.setAnswer(request.getAnswer());
        question.setAnswererId(answererId);
        question.setAnsweredAt(Instant.now());

        question = campaignQuestionRepository.save(question);
        log.info("Question {} answered on campaign {} by issuer {}", questionId, campaignId, answererId);
        return mapToResponse(question);
    }

    @Transactional(readOnly = true)
    public Page<CampaignQuestionResponse> getQuestions(UUID campaignId, boolean answeredOnly, Pageable pageable) {
        campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        Page<CampaignQuestion> questions;
        if (answeredOnly) {
            questions = campaignQuestionRepository.findByCampaignIdAndAnswerIsNotNullOrderByCreatedAtDesc(
                    campaignId, pageable);
        } else {
            questions = campaignQuestionRepository.findByCampaignIdOrderByCreatedAtDesc(campaignId, pageable);
        }

        return questions.map(this::mapToResponse);
    }

    private CampaignQuestionResponse mapToResponse(CampaignQuestion question) {
        return CampaignQuestionResponse.builder()
                .id(question.getId())
                .campaignId(question.getCampaignId())
                .askerId(question.getAskerId())
                .question(question.getQuestion())
                .answer(question.getAnswer())
                .answererId(question.getAnswererId())
                .answeredAt(question.getAnsweredAt())
                .createdAt(question.getCreatedAt())
                .updatedAt(question.getUpdatedAt())
                .build();
    }
}
