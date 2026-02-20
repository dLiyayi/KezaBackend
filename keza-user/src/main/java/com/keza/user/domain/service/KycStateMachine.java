package com.keza.user.domain.service;

import com.keza.common.exception.BusinessRuleException;
import com.keza.user.domain.model.KycDocumentStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class KycStateMachine {

    private static final Map<KycDocumentStatus, Set<KycDocumentStatus>> ALLOWED_TRANSITIONS = Map.of(
            KycDocumentStatus.PENDING, Set.of(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.REJECTED),
            KycDocumentStatus.IN_REVIEW, Set.of(KycDocumentStatus.APPROVED, KycDocumentStatus.REJECTED),
            KycDocumentStatus.REJECTED, Set.of(KycDocumentStatus.PENDING),
            KycDocumentStatus.APPROVED, Set.of()
    );

    /**
     * Validates and returns the target status if the transition is allowed.
     *
     * @param currentStatus the current document status
     * @param targetStatus  the desired target status
     * @throws BusinessRuleException if the transition is not allowed
     */
    public void validateTransition(KycDocumentStatus currentStatus, KycDocumentStatus targetStatus) {
        Set<KycDocumentStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(targetStatus)) {
            throw new BusinessRuleException(
                    "INVALID_KYC_TRANSITION",
                    String.format("Cannot transition KYC document from %s to %s", currentStatus, targetStatus)
            );
        }
    }

    /**
     * Determines the required document types for a user to be KYC-approved.
     * At minimum, a user needs a national ID (or passport) and a selfie.
     */
    public Set<com.keza.common.enums.DocumentType> getRequiredDocumentTypes() {
        return Set.of(
                com.keza.common.enums.DocumentType.NATIONAL_ID,
                com.keza.common.enums.DocumentType.SELFIE
        );
    }
}
