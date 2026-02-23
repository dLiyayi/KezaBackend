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
public class CampaignQuestionRequest {

    @NotBlank(message = "Question is required")
    @Size(max = 2000, message = "Question must be at most 2000 characters")
    private String question;
}
