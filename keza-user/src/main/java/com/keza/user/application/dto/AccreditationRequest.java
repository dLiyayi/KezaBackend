package com.keza.user.application.dto;

import com.keza.common.enums.AccreditationType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccreditationRequest {

    @NotNull(message = "Accreditation type is required")
    private AccreditationType accreditationType;

    @Size(max = 100)
    private String verificationMethod;

    private UUID supportingDocumentId;

    @Size(max = 50)
    private String finraCrdNumber;

    private BigDecimal declaredIncome;

    private BigDecimal declaredNetWorth;
}
