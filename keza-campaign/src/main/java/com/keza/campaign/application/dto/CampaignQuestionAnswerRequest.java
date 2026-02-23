package com.keza.campaign.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignQuestionAnswerRequest {

    @NotBlank(message = "Answer is required")
    @Size(max = 5000, message = "Answer must be at most 5000 characters")
    private String answer;
}
