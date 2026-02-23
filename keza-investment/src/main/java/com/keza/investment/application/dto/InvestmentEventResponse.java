package com.keza.investment.application.dto;

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
public class InvestmentEventResponse {

    private UUID id;
    private UUID investmentId;
    private UUID userId;
    private String eventType;
    private String description;
    private String metadata;
    private Instant createdAt;
}
