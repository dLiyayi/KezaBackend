package com.keza.campaign.application.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CampaignQuestionResponse {

    private UUID id;
    private UUID campaignId;
    private UUID askerId;
    private String question;
    private String answer;
    private UUID answererId;
    private Instant answeredAt;
    private Instant createdAt;
    private Instant updatedAt;
}
