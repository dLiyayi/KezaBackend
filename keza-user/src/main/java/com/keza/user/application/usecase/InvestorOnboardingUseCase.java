package com.keza.user.application.usecase;

import com.keza.common.enums.AccreditationStatus;
import com.keza.common.enums.InvestmentAccountStatus;
import com.keza.common.enums.KycStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.user.application.dto.*;
import com.keza.user.domain.model.AccreditationVerification;
import com.keza.user.domain.model.InvestmentAccount;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.AccreditationVerificationRepository;
import com.keza.user.domain.port.out.InvestmentAccountRepository;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestorOnboardingUseCase {

    private final UserRepository userRepository;
    private final InvestmentAccountRepository accountRepository;
    private final AccreditationVerificationRepository accreditationRepository;

    @Transactional(readOnly = true)
    public InvestorOnboardingStatusResponse getOnboardingStatus(UUID userId) {
        User user = findUser(userId);

        boolean emailVerified = user.isEmailVerified();
        boolean profileComplete = isProfileComplete(user);
        boolean investmentAccountOpen = accountRepository.existsByUserIdAndStatusAndDeletedFalse(userId, InvestmentAccountStatus.OPEN);
        boolean kycApproved = user.getKycStatus() == KycStatus.APPROVED;
        boolean accredited = accreditationRepository.existsByUserIdAndStatus(userId, AccreditationStatus.VERIFIED);
        boolean readyToInvest = emailVerified && profileComplete && investmentAccountOpen && kycApproved;

        String currentPhase;
        String nextStep;

        if (!emailVerified) {
            currentPhase = "ACCOUNT_CREATION";
            nextStep = "Verify your email address";
        } else if (!profileComplete) {
            currentPhase = "ACCOUNT_CREATION";
            nextStep = "Complete your profile (date of birth, gender, country of residence)";
        } else if (!investmentAccountOpen) {
            currentPhase = "INVESTMENT_ACCOUNT";
            nextStep = "Open an investment account with your financial details";
        } else if (!kycApproved) {
            currentPhase = "IDENTITY_VERIFICATION";
            nextStep = user.getKycStatus() == KycStatus.SUBMITTED || user.getKycStatus() == KycStatus.IN_REVIEW
                    ? "Your identity verification is being processed"
                    : "Upload identity documents for KYC verification";
        } else {
            currentPhase = "READY_TO_INVEST";
            nextStep = "Browse campaigns and make your first investment";
        }

        return InvestorOnboardingStatusResponse.builder()
                .emailVerified(emailVerified)
                .profileComplete(profileComplete)
                .investmentAccountOpen(investmentAccountOpen)
                .kycApproved(kycApproved)
                .accredited(accredited)
                .readyToInvest(readyToInvest)
                .currentPhase(currentPhase)
                .nextStep(nextStep)
                .build();
    }

    @Transactional
    public InvestmentAccountResponse openInvestmentAccount(UUID userId, OpenInvestmentAccountRequest request) {
        User user = findUser(userId);

        if (!user.isEmailVerified()) {
            throw new BusinessRuleException("Email must be verified before opening an investment account");
        }

        if (accountRepository.existsByUserIdAndStatusAndDeletedFalse(userId, InvestmentAccountStatus.OPEN)) {
            throw new BusinessRuleException("You already have an open investment account");
        }

        InvestmentAccount account = InvestmentAccount.builder()
                .userId(userId)
                .accountType(request.getAccountType())
                .citizenship(request.getCitizenship())
                .maritalStatus(request.getMaritalStatus())
                .employmentStatus(request.getEmploymentStatus())
                .annualIncome(request.getAnnualIncome())
                .netWorth(request.getNetWorth())
                .investmentExperience(request.getInvestmentExperience())
                .riskTolerance(request.getRiskTolerance())
                .status(InvestmentAccountStatus.PENDING)
                .build();

        account = accountRepository.save(account);

        // Auto-open for individual accounts with complete data
        if (request.getAccountType() == com.keza.common.enums.AccountType.INDIVIDUAL) {
            account.setStatus(InvestmentAccountStatus.OPEN);
            account.setOpenedAt(Instant.now());
            account = accountRepository.save(account);
        }

        log.info("Investment account opened for user {}: type={}, status={}", userId, request.getAccountType(), account.getStatus());
        return toAccountResponse(account);
    }

    @Transactional(readOnly = true)
    public List<InvestmentAccountResponse> getMyAccounts(UUID userId) {
        List<InvestmentAccount> accounts = accountRepository.findByUserIdAndDeletedFalse(userId);
        return accounts.stream().map(this::toAccountResponse).toList();
    }

    @Transactional(readOnly = true)
    public InvestmentAccountResponse getAccountDetail(UUID accountId) {
        InvestmentAccount account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("InvestmentAccount", accountId));
        return toAccountResponse(account);
    }

    @Transactional
    public InvestmentAccountResponse adminReviewAccount(UUID accountId, UUID adminId, boolean approved, String notes) {
        InvestmentAccount account = accountRepository.findByIdAndDeletedFalse(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("InvestmentAccount", accountId));

        if (account.getStatus() != InvestmentAccountStatus.PENDING) {
            throw new BusinessRuleException("Account is not in a reviewable state");
        }

        if (approved) {
            account.setStatus(InvestmentAccountStatus.OPEN);
            account.setOpenedAt(Instant.now());
        } else {
            account.setStatus(InvestmentAccountStatus.SUSPENDED);
        }
        account.setProcessingNotes(notes);
        account = accountRepository.save(account);

        log.info("Investment account {} reviewed by admin {}: approved={}", accountId, adminId, approved);
        return toAccountResponse(account);
    }

    @Transactional
    public AccreditationResponse submitAccreditation(UUID userId, AccreditationRequest request) {
        findUser(userId);

        AccreditationVerification verification = AccreditationVerification.builder()
                .userId(userId)
                .accreditationType(request.getAccreditationType())
                .verificationMethod(request.getVerificationMethod())
                .supportingDocumentId(request.getSupportingDocumentId())
                .finraCrdNumber(request.getFinraCrdNumber())
                .verifiedIncome(request.getDeclaredIncome())
                .verifiedNetWorth(request.getDeclaredNetWorth())
                .status(AccreditationStatus.SUBMITTED)
                .build();

        verification = accreditationRepository.save(verification);
        log.info("Accreditation submitted for user {}: type={}", userId, request.getAccreditationType());
        return toAccreditationResponse(verification);
    }

    @Transactional(readOnly = true)
    public List<AccreditationResponse> getMyAccreditations(UUID userId) {
        List<AccreditationVerification> verifications = accreditationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return verifications.stream().map(this::toAccreditationResponse).toList();
    }

    @Transactional
    public AccreditationResponse adminReviewAccreditation(UUID verificationId, UUID adminId, boolean approved, String notes) {
        AccreditationVerification verification = accreditationRepository.findById(verificationId)
                .orElseThrow(() -> new ResourceNotFoundException("AccreditationVerification", verificationId));

        if (verification.getStatus() != AccreditationStatus.SUBMITTED && verification.getStatus() != AccreditationStatus.PENDING) {
            throw new BusinessRuleException("Accreditation is not in a reviewable state");
        }

        verification.setReviewerId(adminId);
        verification.setReviewNotes(notes);

        if (approved) {
            verification.setStatus(AccreditationStatus.VERIFIED);
            verification.setVerifiedAt(Instant.now());
            verification.setExpiresAt(Instant.now().plus(365, ChronoUnit.DAYS));
        } else {
            verification.setStatus(AccreditationStatus.REJECTED);
            verification.setRejectedReason(notes);
        }

        verification = accreditationRepository.save(verification);
        log.info("Accreditation {} reviewed by admin {}: approved={}", verificationId, adminId, approved);
        return toAccreditationResponse(verification);
    }

    @Transactional(readOnly = true)
    public List<AccreditationResponse> getPendingAccreditations() {
        List<AccreditationVerification> pending = accreditationRepository.findByStatus(AccreditationStatus.SUBMITTED);
        return pending.stream().map(this::toAccreditationResponse).toList();
    }

    private boolean isProfileComplete(User user) {
        return user.getDateOfBirth() != null
                && user.getGender() != null
                && user.getCountryOfResidence() != null
                && user.getFirstName() != null
                && user.getLastName() != null;
    }

    private User findUser(UUID userId) {
        return userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private InvestmentAccountResponse toAccountResponse(InvestmentAccount account) {
        return InvestmentAccountResponse.builder()
                .id(account.getId())
                .userId(account.getUserId())
                .accountType(account.getAccountType())
                .status(account.getStatus())
                .citizenship(account.getCitizenship())
                .maritalStatus(account.getMaritalStatus())
                .employmentStatus(account.getEmploymentStatus())
                .annualIncome(account.getAnnualIncome())
                .netWorth(account.getNetWorth())
                .investmentExperience(account.getInvestmentExperience())
                .riskTolerance(account.getRiskTolerance())
                .openedAt(account.getOpenedAt())
                .createdAt(account.getCreatedAt())
                .updatedAt(account.getUpdatedAt())
                .build();
    }

    private AccreditationResponse toAccreditationResponse(AccreditationVerification v) {
        return AccreditationResponse.builder()
                .id(v.getId())
                .userId(v.getUserId())
                .accreditationType(v.getAccreditationType())
                .status(v.getStatus())
                .verificationMethod(v.getVerificationMethod())
                .supportingDocumentId(v.getSupportingDocumentId())
                .finraCrdNumber(v.getFinraCrdNumber())
                .verifiedIncome(v.getVerifiedIncome())
                .verifiedNetWorth(v.getVerifiedNetWorth())
                .verifiedAt(v.getVerifiedAt())
                .expiresAt(v.getExpiresAt())
                .rejectedReason(v.getRejectedReason())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
