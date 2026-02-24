package com.keza.ai.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentValidationRequest {

    @NotNull(message = "Document ID is required")
    private UUID documentId;

    private String documentType; // NATIONAL_ID, PASSPORT, DRIVERS_LICENSE, CERTIFICATE_OF_INCORPORATION, TAX_COMPLIANCE
}
