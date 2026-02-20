package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.CreateInvestmentRequest;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.domain.event.InvestmentCreatedEvent;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.model.Transaction;
import com.keza.investment.domain.model.TransactionType;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.investment.domain.port.out.TransactionRepository;
import com.keza.investment.domain.service.InvestmentValidator;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvestmentUseCase {

    private static final Duration COOLING_OFF_PERIOD = Duration.ofHours(48);

    private final InvestmentRepository investmentRepository;
    private final TransactionRepository transactionRepository;
    private final CampaignRepository campaignRepository;
    private final UserRepository userRepository;
    private final InvestmentValidator investmentValidator;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InvestmentResponse createInvestment(UUID investorId, CreateInvestmentRequest request) {
        log.info("Creating investment for investor {} in campaign {}", investorId, request.getCampaignId());

        User investor = userRepository.findByIdAndDeletedFalse(investorId)
                .orElseThrow(() -> new ResourceNotFoundException("User", investorId));

        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(request.getCampaignId())
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", request.getCampaignId()));

        boolean alreadyInvested = investmentRepository.existsByInvestorIdAndCampaignId(
                investorId, request.getCampaignId());

        investmentValidator.validate(investorId, campaign, request.getAmount(),
                investor.getKycStatus(), alreadyInvested);

        PaymentMethod paymentMethod = PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());

        BigDecimal sharePrice = campaign.getSharePrice();
        long shares = request.getAmount()
                .divide(sharePrice, 0, RoundingMode.FLOOR)
                .longValue();

        if (shares <= 0) {
            throw new BusinessRuleException("INSUFFICIENT_AMOUNT",
                    "Investment amount is too small to purchase any shares at price " + sharePrice);
        }

        BigDecimal actualAmount = sharePrice.multiply(BigDecimal.valueOf(shares));

        Investment investment = Investment.builder()
                .investorId(investorId)
                .campaignId(request.getCampaignId())
                .amount(actualAmount)
                .shares(shares)
                .sharePrice(sharePrice)
                .status(InvestmentStatus.PENDING)
                .paymentMethod(paymentMethod)
                .coolingOffExpiresAt(Instant.now().plus(COOLING_OFF_PERIOD))
                .build();

        investment = investmentRepository.save(investment);

        Transaction transaction = Transaction.builder()
                .investmentId(investment.getId())
                .userId(investorId)
                .type(TransactionType.INVESTMENT)
                .amount(actualAmount)
                .currency("KES")
                .paymentMethod(paymentMethod)
                .description("Investment in campaign: " + campaign.getTitle())
                .build();

        transactionRepository.save(transaction);

        int updatedRows = campaignRepository.updateRaisedAmount(
                campaign.getId(), actualAmount, shares, campaign.getVersion());

        if (updatedRows == 0) {
            throw new BusinessRuleException("CONCURRENT_UPDATE",
                    "Campaign was updated by another transaction. Please try again.");
        }

        eventPublisher.publishEvent(new InvestmentCreatedEvent(
                investment.getId(), investorId, campaign.getId(), actualAmount));

        log.info("Investment {} created successfully: {} shares at {} = {}",
                investment.getId(), shares, sharePrice, actualAmount);

        return mapToResponse(investment);
    }

    @Transactional
    public InvestmentResponse cancelInvestment(UUID investmentId, UUID userId) {
        log.info("Cancelling investment {} by user {}", investmentId, userId);

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", investmentId));

        if (!investment.getInvestorId().equals(userId)) {
            throw new BusinessRuleException("UNAUTHORIZED",
                    "You are not authorized to cancel this investment");
        }

        if (investment.getStatus() != InvestmentStatus.PENDING
                && investment.getStatus() != InvestmentStatus.COOLING_OFF) {
            throw new BusinessRuleException("INVALID_STATUS",
                    "Investment cannot be cancelled in status: " + investment.getStatus());
        }

        if (investment.getCoolingOffExpiresAt() != null
                && Instant.now().isAfter(investment.getCoolingOffExpiresAt())) {
            throw new BusinessRuleException("COOLING_OFF_EXPIRED",
                    "The 48-hour cooling-off period has expired. This investment can no longer be cancelled.");
        }

        investment.setStatus(InvestmentStatus.CANCELLED);
        investment.setCancelledAt(Instant.now());
        investment.setCancellationReason("Cancelled by investor within cooling-off period");

        UUID campaignId = investment.getCampaignId();
        BigDecimal investmentAmount = investment.getAmount();
        long investmentShares = investment.getShares();
        investmentRepository.save(investment);

        Campaign campaign = campaignRepository.findByIdAndDeletedFalse(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign", campaignId));

        campaignRepository.updateRaisedAmount(
                campaign.getId(),
                investmentAmount.negate(),
                -investmentShares,
                campaign.getVersion());

        Transaction refundTransaction = Transaction.builder()
                .investmentId(investmentId)
                .userId(userId)
                .type(TransactionType.REFUND)
                .amount(investmentAmount)
                .currency("KES")
                .paymentMethod(investment.getPaymentMethod())
                .description("Refund for cancelled investment in campaign")
                .build();

        transactionRepository.save(refundTransaction);

        log.info("Investment {} cancelled successfully", investmentId);
        return mapToResponse(investment);
    }

    @Transactional
    public InvestmentResponse completeInvestment(UUID investmentId) {
        log.info("Completing investment {}", investmentId);

        Investment investment = investmentRepository.findById(investmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", investmentId));

        if (investment.getStatus() != InvestmentStatus.PENDING
                && investment.getStatus() != InvestmentStatus.COOLING_OFF
                && investment.getStatus() != InvestmentStatus.PAYMENT_INITIATED) {
            throw new BusinessRuleException("INVALID_STATUS",
                    "Investment cannot be completed from status: " + investment.getStatus());
        }

        investment.setStatus(InvestmentStatus.COMPLETED);
        investment.setCompletedAt(Instant.now());
        investment = investmentRepository.save(investment);

        log.info("Investment {} completed successfully", investmentId);
        return mapToResponse(investment);
    }

    @Transactional(readOnly = true)
    public InvestmentResponse getInvestment(UUID id) {
        Investment investment = investmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Investment", id));
        return mapToResponse(investment);
    }

    @Transactional(readOnly = true)
    public Page<InvestmentResponse> getUserInvestments(UUID userId, Pageable pageable) {
        return investmentRepository.findByInvestorIdOrderByCreatedAtDesc(userId, pageable)
                .map(this::mapToResponse);
    }

    private InvestmentResponse mapToResponse(Investment investment) {
        return InvestmentResponse.builder()
                .id(investment.getId())
                .investorId(investment.getInvestorId())
                .campaignId(investment.getCampaignId())
                .amount(investment.getAmount())
                .shares(investment.getShares())
                .sharePrice(investment.getSharePrice())
                .status(investment.getStatus().name())
                .paymentMethod(investment.getPaymentMethod() != null
                        ? investment.getPaymentMethod().name() : null)
                .coolingOffExpiresAt(investment.getCoolingOffExpiresAt())
                .completedAt(investment.getCompletedAt())
                .cancelledAt(investment.getCancelledAt())
                .cancellationReason(investment.getCancellationReason())
                .createdAt(investment.getCreatedAt())
                .updatedAt(investment.getUpdatedAt())
                .build();
    }
}
