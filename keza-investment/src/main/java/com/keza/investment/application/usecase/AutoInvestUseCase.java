package com.keza.investment.application.usecase;

import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.AutoInvestPreferenceRequest;
import com.keza.investment.application.dto.AutoInvestPreferenceResponse;
import com.keza.investment.domain.model.AutoInvestPreference;
import com.keza.investment.domain.port.out.AutoInvestPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoInvestUseCase {

    private final AutoInvestPreferenceRepository autoInvestPreferenceRepository;

    @Transactional
    public AutoInvestPreferenceResponse createOrUpdatePreference(UUID userId, AutoInvestPreferenceRequest request) {
        AutoInvestPreference preference = autoInvestPreferenceRepository.findByUserId(userId)
                .orElse(AutoInvestPreference.builder()
                        .userId(userId)
                        .build());

        preference.setBudgetAmount(request.getBudgetAmount());
        preference.setRemainingBudget(request.getBudgetAmount());
        preference.setMaxPerCampaign(request.getMaxPerCampaign());
        preference.setIndustries(request.getIndustries());
        preference.setMinTargetAmount(request.getMinTargetAmount());
        preference.setMaxTargetAmount(request.getMaxTargetAmount());
        preference.setOfferingTypes(request.getOfferingTypes());

        preference = autoInvestPreferenceRepository.save(preference);
        log.info("Auto-invest preference saved for user {}", userId);
        return mapToResponse(preference);
    }

    @Transactional(readOnly = true)
    public AutoInvestPreferenceResponse getPreference(UUID userId) {
        AutoInvestPreference preference = autoInvestPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AutoInvestPreference", userId));
        return mapToResponse(preference);
    }

    @Transactional
    public AutoInvestPreferenceResponse toggleAutoInvest(UUID userId, boolean enabled) {
        AutoInvestPreference preference = autoInvestPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AutoInvestPreference", userId));

        if (enabled && preference.getRemainingBudget().signum() <= 0) {
            throw new BusinessRuleException("INSUFFICIENT_BUDGET",
                    "Cannot enable auto-invest with zero remaining budget");
        }

        preference.setEnabled(enabled);
        preference = autoInvestPreferenceRepository.save(preference);
        log.info("Auto-invest {} for user {}", enabled ? "enabled" : "disabled", userId);
        return mapToResponse(preference);
    }

    @Transactional
    public void deletePreference(UUID userId) {
        AutoInvestPreference preference = autoInvestPreferenceRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("AutoInvestPreference", userId));
        autoInvestPreferenceRepository.delete(preference);
        log.info("Auto-invest preference deleted for user {}", userId);
    }

    private AutoInvestPreferenceResponse mapToResponse(AutoInvestPreference preference) {
        return AutoInvestPreferenceResponse.builder()
                .id(preference.getId())
                .userId(preference.getUserId())
                .enabled(preference.isEnabled())
                .budgetAmount(preference.getBudgetAmount())
                .remainingBudget(preference.getRemainingBudget())
                .maxPerCampaign(preference.getMaxPerCampaign())
                .industries(preference.getIndustries())
                .minTargetAmount(preference.getMinTargetAmount())
                .maxTargetAmount(preference.getMaxTargetAmount())
                .offeringTypes(preference.getOfferingTypes())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
