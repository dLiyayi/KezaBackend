package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.InvestmentCertificateResponse;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvestmentCertificateUseCase {

    private final InvestmentRepository investmentRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public InvestmentCertificateResponse generateCertificate(UUID investmentId, UUID userId) {
        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", investmentId));

        if (!investment.getInvestorId().equals(userId)) {
            throw new BusinessRuleException("FORBIDDEN", "You do not own this investment");
        }

        if (investment.getStatus() != InvestmentStatus.COMPLETED
                && investment.getStatus() != InvestmentStatus.COOLING_OFF) {
            throw new BusinessRuleException("INVALID_STATUS",
                    "Certificates are only available for completed or active investments");
        }

        User investor = userRepository.findByIdAndDeletedFalse(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));

        Campaign campaign = campaignRepository.findById(investment.getCampaignId()).orElse(null);

        String certificateNumber = "KEZA-" + investmentId.toString().substring(0, 8).toUpperCase()
                + "-" + investment.getCreatedAt().toEpochMilli() % 100000;

        return InvestmentCertificateResponse.builder()
                .certificateId(UUID.randomUUID())
                .investmentId(investmentId)
                .investorName(investor.getFirstName() + " " + investor.getLastName())
                .campaignTitle(campaign != null ? campaign.getTitle() : null)
                .companyName(campaign != null ? campaign.getCompanyName() : null)
                .industry(campaign != null ? campaign.getIndustry() : null)
                .investmentAmount(investment.getAmount())
                .shares(investment.getShares())
                .sharePrice(investment.getSharePrice())
                .currency("KES")
                .investmentDate(investment.getCreatedAt())
                .completedDate(investment.getCompletedAt())
                .status(investment.getStatus().name())
                .certificateNumber(certificateNumber)
                .issuedAt(Instant.now())
                .build();
    }
}
