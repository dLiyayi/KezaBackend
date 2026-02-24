package com.keza.campaign.application.usecase;

import com.keza.campaign.application.dto.IssuerApplicationRequest;
import com.keza.campaign.application.dto.IssuerApplicationResponse;
import com.keza.campaign.application.dto.IssuerOnboardingStepResponse;
import com.keza.campaign.domain.model.IssuerApplication;
import com.keza.campaign.domain.model.IssuerOnboardingStep;
import com.keza.campaign.domain.port.out.IssuerApplicationRepository;
import com.keza.campaign.domain.port.out.IssuerOnboardingStepRepository;
import com.keza.common.enums.IssuerApplicationStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class IssuerOnboardingUseCase {

    private final IssuerApplicationRepository applicationRepository;
    private final IssuerOnboardingStepRepository stepRepository;

    private static final List<String[]> ONBOARDING_STEPS = List.of(
            new String[]{"1", "Application Submitted"},
            new String[]{"1", "Eligibility Review"},
            new String[]{"1", "Account Manager Assignment"},
            new String[]{"2", "Regulatory Filing"},
            new String[]{"2", "Due Diligence (144-Point)"},
            new String[]{"2", "Securities Structure"},
            new String[]{"3", "Campaign Strategist Assignment"},
            new String[]{"3", "Pitch & Narrative"},
            new String[]{"3", "Campaign Builder (6-Step Wizard)"},
            new String[]{"3", "Perks & Rewards"},
            new String[]{"4", "Soft Launch (Testing the Waters)"},
            new String[]{"4", "Marketing Strategy"},
            new String[]{"5", "Campaign Goes Live"},
            new String[]{"5", "Investor Management"},
            new String[]{"5", "Rolling Closes & Disbursement"}
    );

    @Transactional
    public IssuerApplicationResponse submitApplication(UUID userId, IssuerApplicationRequest request) {
        Set<IssuerApplicationStatus> activeStatuses = Set.of(
                IssuerApplicationStatus.SUBMITTED,
                IssuerApplicationStatus.UNDER_REVIEW,
                IssuerApplicationStatus.ELIGIBLE
        );

        if (applicationRepository.existsByUserIdAndStatusInAndDeletedFalse(userId, activeStatuses)) {
            throw new BusinessRuleException("You already have an active issuer application");
        }

        IssuerApplication application = IssuerApplication.builder()
                .userId(userId)
                .companyName(request.getCompanyName())
                .companyRegistrationNumber(request.getCompanyRegistrationNumber())
                .companyWebsite(request.getCompanyWebsite())
                .industry(request.getIndustry())
                .businessStage(request.getBusinessStage())
                .fundingGoal(request.getFundingGoal())
                .regulationType(request.getRegulationType())
                .pitchSummary(request.getPitchSummary())
                .status(IssuerApplicationStatus.SUBMITTED)
                .build();

        application = applicationRepository.save(application);

        initializeOnboardingSteps(application.getId());

        // Mark "Application Submitted" step as completed
        List<IssuerOnboardingStep> steps = stepRepository.findByApplicationIdOrderByPhaseAscStepNameAsc(application.getId());
        steps.stream()
                .filter(s -> "Application Submitted".equals(s.getStepName()))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus("COMPLETED");
                    step.setCompletedAt(Instant.now());
                    step.setCompletedBy(userId);
                    stepRepository.save(step);
                });

        log.info("Issuer application submitted by user: {}, company: {}", userId, request.getCompanyName());

        return toResponse(application);
    }

    @Transactional(readOnly = true)
    public IssuerApplicationResponse getMyApplication(UUID userId) {
        IssuerApplication application = applicationRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", "userId", userId.toString()));
        return toResponseWithSteps(application);
    }

    @Transactional(readOnly = true)
    public Page<IssuerApplicationResponse> getApplicationsForReview(IssuerApplicationStatus status, Pageable pageable) {
        Page<IssuerApplication> applications;
        if (status != null) {
            applications = applicationRepository.findByStatusAndDeletedFalse(status, pageable);
        } else {
            applications = applicationRepository.findByDeletedFalse(pageable);
        }
        return applications.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public IssuerApplicationResponse getApplicationDetail(UUID applicationId) {
        IssuerApplication application = applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", applicationId));
        return toResponseWithSteps(application);
    }

    @Transactional
    public IssuerApplicationResponse reviewApplication(UUID applicationId, UUID adminId, boolean approved, String notes) {
        IssuerApplication application = applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", applicationId));

        if (application.getStatus() != IssuerApplicationStatus.SUBMITTED &&
                application.getStatus() != IssuerApplicationStatus.UNDER_REVIEW) {
            throw new BusinessRuleException("Application is not in a reviewable state");
        }

        application.setReviewerId(adminId);
        application.setReviewNotes(notes);
        application.setReviewedAt(Instant.now());

        if (approved) {
            application.setStatus(IssuerApplicationStatus.ELIGIBLE);
            application.setEligibleAt(Instant.now());
            markStep(applicationId, "Eligibility Review", "COMPLETED", adminId);
        } else {
            application.setStatus(IssuerApplicationStatus.REJECTED);
            application.setRejectedReason(notes);
        }

        application = applicationRepository.save(application);
        log.info("Application {} reviewed by admin {}: approved={}", applicationId, adminId, approved);

        return toResponseWithSteps(application);
    }

    @Transactional
    public IssuerApplicationResponse assignAccountManager(UUID applicationId, UUID adminId, UUID managerId) {
        IssuerApplication application = applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", applicationId));

        application.setAccountManagerId(managerId);
        application = applicationRepository.save(application);

        markStep(applicationId, "Account Manager Assignment", "COMPLETED", adminId);
        log.info("Account manager {} assigned to application {} by admin {}", managerId, applicationId, adminId);

        return toResponseWithSteps(application);
    }

    @Transactional
    public IssuerOnboardingStepResponse updateOnboardingStep(UUID applicationId, UUID stepId, String status, String notes) {
        applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", applicationId));

        IssuerOnboardingStep step = stepRepository.findById(stepId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerOnboardingStep", stepId));

        if (!step.getApplicationId().equals(applicationId)) {
            throw new BusinessRuleException("Step does not belong to this application");
        }

        step.setStatus(status);
        step.setNotes(notes);
        if ("COMPLETED".equals(status)) {
            step.setCompletedAt(Instant.now());
        }

        step = stepRepository.save(step);
        return toStepResponse(step);
    }

    @Transactional(readOnly = true)
    public List<IssuerOnboardingStepResponse> getOnboardingProgress(UUID applicationId) {
        applicationRepository.findByIdAndDeletedFalse(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", applicationId));

        List<IssuerOnboardingStep> steps = stepRepository.findByApplicationIdOrderByPhaseAscStepNameAsc(applicationId);
        return steps.stream().map(this::toStepResponse).toList();
    }

    @Transactional
    public void withdrawApplication(UUID userId) {
        IssuerApplication application = applicationRepository.findByUserIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("IssuerApplication", "userId", userId.toString()));

        if (application.getStatus() == IssuerApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleException("Application is already withdrawn");
        }

        application.setStatus(IssuerApplicationStatus.WITHDRAWN);
        applicationRepository.save(application);
        log.info("Issuer application withdrawn by user: {}", userId);
    }

    private void initializeOnboardingSteps(UUID applicationId) {
        for (String[] stepDef : ONBOARDING_STEPS) {
            IssuerOnboardingStep step = IssuerOnboardingStep.builder()
                    .applicationId(applicationId)
                    .phase(Integer.parseInt(stepDef[0]))
                    .stepName(stepDef[1])
                    .status("PENDING")
                    .build();
            stepRepository.save(step);
        }
    }

    private void markStep(UUID applicationId, String stepName, String status, UUID completedBy) {
        List<IssuerOnboardingStep> steps = stepRepository.findByApplicationIdOrderByPhaseAscStepNameAsc(applicationId);
        steps.stream()
                .filter(s -> stepName.equals(s.getStepName()))
                .findFirst()
                .ifPresent(step -> {
                    step.setStatus(status);
                    step.setCompletedAt(Instant.now());
                    step.setCompletedBy(completedBy);
                    stepRepository.save(step);
                });
    }

    private IssuerApplicationResponse toResponse(IssuerApplication application) {
        return IssuerApplicationResponse.builder()
                .id(application.getId())
                .userId(application.getUserId())
                .companyName(application.getCompanyName())
                .companyRegistrationNumber(application.getCompanyRegistrationNumber())
                .companyWebsite(application.getCompanyWebsite())
                .industry(application.getIndustry())
                .businessStage(application.getBusinessStage())
                .fundingGoal(application.getFundingGoal())
                .regulationType(application.getRegulationType())
                .pitchSummary(application.getPitchSummary())
                .status(application.getStatus())
                .accountManagerId(application.getAccountManagerId())
                .reviewerId(application.getReviewerId())
                .reviewNotes(application.getReviewNotes())
                .reviewedAt(application.getReviewedAt())
                .eligibleAt(application.getEligibleAt())
                .rejectedReason(application.getRejectedReason())
                .createdAt(application.getCreatedAt())
                .updatedAt(application.getUpdatedAt())
                .build();
    }

    private IssuerApplicationResponse toResponseWithSteps(IssuerApplication application) {
        IssuerApplicationResponse response = toResponse(application);
        List<IssuerOnboardingStep> steps = stepRepository.findByApplicationIdOrderByPhaseAscStepNameAsc(application.getId());
        response.setOnboardingSteps(steps.stream().map(this::toStepResponse).toList());
        return response;
    }

    private IssuerOnboardingStepResponse toStepResponse(IssuerOnboardingStep step) {
        return IssuerOnboardingStepResponse.builder()
                .id(step.getId())
                .phase(step.getPhase())
                .stepName(step.getStepName())
                .status(step.getStatus())
                .completedAt(step.getCompletedAt())
                .completedBy(step.getCompletedBy())
                .notes(step.getNotes())
                .build();
    }
}
