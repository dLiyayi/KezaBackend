package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.enums.KycStatus;
import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.common.exception.ResourceNotFoundException;
import com.keza.investment.application.dto.CreateInvestmentRequest;
import com.keza.investment.application.dto.InvestmentResponse;
import com.keza.investment.domain.event.InvestmentCreatedEvent;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.model.Transaction;
import com.keza.investment.domain.port.out.InvestmentRepository;
import com.keza.investment.domain.port.out.TransactionRepository;
import com.keza.investment.domain.service.InvestmentValidator;
import com.keza.user.domain.model.User;
import com.keza.user.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InvestmentUseCase")
class InvestmentUseCaseTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private CampaignRepository campaignRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private InvestmentValidator investmentValidator;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private InvestmentEventUseCase investmentEventUseCase;

    @InjectMocks
    private InvestmentUseCase investmentUseCase;

    @Captor
    private ArgumentCaptor<Investment> investmentCaptor;
    @Captor
    private ArgumentCaptor<Transaction> transactionCaptor;
    @Captor
    private ArgumentCaptor<InvestmentCreatedEvent> eventCaptor;

    private UUID investorId;
    private UUID campaignId;
    private User investor;
    private Campaign campaign;
    private CreateInvestmentRequest request;

    @BeforeEach
    void setUp() {
        investorId = UUID.randomUUID();
        campaignId = UUID.randomUUID();

        investor = User.builder()
                .email("investor@example.com")
                .firstName("Jane")
                .lastName("Doe")
                .passwordHash("hashed")
                .kycStatus(KycStatus.APPROVED)
                .build();
        investor.setId(investorId);

        campaign = Campaign.builder()
                .issuerId(UUID.randomUUID())
                .title("Green Energy Fund")
                .status(CampaignStatus.LIVE)
                .targetAmount(new BigDecimal("1000000"))
                .raisedAmount(BigDecimal.ZERO)
                .sharePrice(new BigDecimal("100"))
                .minInvestment(new BigDecimal("1000"))
                .maxInvestment(new BigDecimal("500000"))
                .endDate(Instant.now().plus(30, ChronoUnit.DAYS))
                .build();
        campaign.setId(campaignId);
        campaign.setVersion(1L);

        request = CreateInvestmentRequest.builder()
                .campaignId(campaignId)
                .amount(new BigDecimal("10000"))
                .paymentMethod("MPESA")
                .build();
    }

    @Nested
    @DisplayName("createInvestment")
    class CreateInvestment {

        @Test
        @DisplayName("should create investment successfully with correct share calculation")
        void shouldCreateInvestmentSuccessfully() {
            // Arrange
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(investmentRepository.existsByInvestorIdAndCampaignId(investorId, campaignId)).thenReturn(false);
            when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
                Investment inv = invocation.getArgument(0);
                inv.setId(UUID.randomUUID());
                return inv;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(campaignRepository.updateRaisedAmount(eq(campaignId), any(), eq(100L), eq(1L))).thenReturn(1);

            // Act
            InvestmentResponse response = investmentUseCase.createInvestment(investorId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getInvestorId()).isEqualTo(investorId);
            assertThat(response.getCampaignId()).isEqualTo(campaignId);
            assertThat(response.getShares()).isEqualTo(100); // 10000 / 100 = 100 shares
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000")); // 100 * 100
            assertThat(response.getSharePrice()).isEqualByComparingTo(new BigDecimal("100"));
            assertThat(response.getStatus()).isEqualTo("PENDING");
            assertThat(response.getPaymentMethod()).isEqualTo("MPESA");

            verify(investmentValidator).validate(eq(investorId), eq(campaign),
                    eq(new BigDecimal("10000")), eq(KycStatus.APPROVED), eq(false));
            verify(investmentRepository).save(investmentCaptor.capture());
            assertThat(investmentCaptor.getValue().getCoolingOffExpiresAt()).isNotNull();

            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction savedTx = transactionCaptor.getValue();
            assertThat(savedTx.getCurrency()).isEqualTo("KES");
            assertThat(savedTx.getPaymentMethod()).isEqualTo(PaymentMethod.MPESA);

            verify(eventPublisher).publishEvent(eventCaptor.capture());
            InvestmentCreatedEvent event = eventCaptor.getValue();
            assertThat(event.investorId()).isEqualTo(investorId);
            assertThat(event.campaignId()).isEqualTo(campaignId);
            assertThat(event.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should floor share count when amount is not exact multiple of share price")
        void shouldFloorShareCount() {
            request.setAmount(new BigDecimal("10050")); // 10050 / 100 = 100 shares (floor), actual = 10000

            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(investmentRepository.existsByInvestorIdAndCampaignId(investorId, campaignId)).thenReturn(false);
            when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
                Investment inv = invocation.getArgument(0);
                inv.setId(UUID.randomUUID());
                return inv;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(campaignRepository.updateRaisedAmount(eq(campaignId), any(), eq(100L), eq(1L))).thenReturn(1);

            InvestmentResponse response = investmentUseCase.createInvestment(investorId, request);

            assertThat(response.getShares()).isEqualTo(100);
            assertThat(response.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should throw INSUFFICIENT_AMOUNT when amount is less than one share price")
        void shouldThrowWhenAmountTooSmallForOneShare() {
            request.setAmount(new BigDecimal("50")); // 50 / 100 = 0 shares
            campaign.setMinInvestment(new BigDecimal("1")); // bypass min check in validator

            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(investmentRepository.existsByInvestorIdAndCampaignId(investorId, campaignId)).thenReturn(false);

            assertThatThrownBy(() -> investmentUseCase.createInvestment(investorId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("too small to purchase any shares");
        }

        @Test
        @DisplayName("should throw CONCURRENT_UPDATE when campaign version has changed")
        void shouldThrowOnConcurrentUpdate() {
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(investmentRepository.existsByInvestorIdAndCampaignId(investorId, campaignId)).thenReturn(false);
            when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> {
                Investment inv = invocation.getArgument(0);
                inv.setId(UUID.randomUUID());
                return inv;
            });
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(campaignRepository.updateRaisedAmount(any(), any(), anyLong(), anyLong())).thenReturn(0);

            assertThatThrownBy(() -> investmentUseCase.createInvestment(investorId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Campaign was updated by another transaction");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when investor not found")
        void shouldThrowWhenInvestorNotFound() {
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> investmentUseCase.createInvestment(investorId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("User");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when campaign not found")
        void shouldThrowWhenCampaignNotFound() {
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> investmentUseCase.createInvestment(investorId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Campaign");
        }

        @Test
        @DisplayName("should propagate validation exception from InvestmentValidator")
        void shouldPropagateValidationException() {
            when(userRepository.findByIdAndDeletedFalse(investorId)).thenReturn(Optional.of(investor));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(investmentRepository.existsByInvestorIdAndCampaignId(investorId, campaignId)).thenReturn(false);
            doThrow(new BusinessRuleException("KYC_NOT_APPROVED", "KYC not approved"))
                    .when(investmentValidator).validate(any(), any(), any(), any(), anyBoolean());

            assertThatThrownBy(() -> investmentUseCase.createInvestment(investorId, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("KYC not approved");
        }
    }

    @Nested
    @DisplayName("cancelInvestment")
    class CancelInvestment {

        private UUID investmentId;
        private Investment existingInvestment;

        @BeforeEach
        void setUp() {
            investmentId = UUID.randomUUID();
            existingInvestment = Investment.builder()
                    .investorId(investorId)
                    .campaignId(campaignId)
                    .amount(new BigDecimal("10000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("100"))
                    .status(InvestmentStatus.PENDING)
                    .paymentMethod(PaymentMethod.MPESA)
                    .coolingOffExpiresAt(Instant.now().plus(48, ChronoUnit.HOURS))
                    .build();
            existingInvestment.setId(investmentId);
        }

        @Test
        @DisplayName("should cancel investment within cooling-off period successfully")
        void shouldCancelWithinCoolingOff() {
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));
            when(investmentRepository.save(any(Investment.class))).thenAnswer(invocation -> invocation.getArgument(0));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.updateRaisedAmount(any(), any(), anyLong(), anyLong())).thenReturn(1);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));

            InvestmentResponse response = investmentUseCase.cancelInvestment(investmentId, investorId);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");

            verify(investmentRepository).save(investmentCaptor.capture());
            Investment saved = investmentCaptor.getValue();
            assertThat(saved.getStatus()).isEqualTo(InvestmentStatus.CANCELLED);
            assertThat(saved.getCancelledAt()).isNotNull();
            assertThat(saved.getCancellationReason()).contains("cooling-off");

            verify(campaignRepository).updateRaisedAmount(
                    eq(campaignId),
                    eq(new BigDecimal("10000").negate()),
                    eq(-100L),
                    eq(campaign.getVersion()));

            verify(transactionRepository).save(transactionCaptor.capture());
            Transaction refund = transactionCaptor.getValue();
            assertThat(refund.getType()).isEqualTo(com.keza.investment.domain.model.TransactionType.REFUND);
            assertThat(refund.getAmount()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should throw COOLING_OFF_EXPIRED when cooling-off period has passed")
        void shouldThrowWhenCoolingOffExpired() {
            existingInvestment.setCoolingOffExpiresAt(Instant.now().minus(1, ChronoUnit.HOURS));

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));

            assertThatThrownBy(() -> investmentUseCase.cancelInvestment(investmentId, investorId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cooling-off period has expired");
        }

        @Test
        @DisplayName("should throw INVALID_STATUS when investment status is COMPLETED")
        void shouldThrowWhenInvalidStatus() {
            existingInvestment.setStatus(InvestmentStatus.COMPLETED);

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));

            assertThatThrownBy(() -> investmentUseCase.cancelInvestment(investmentId, investorId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be cancelled in status");
        }

        @Test
        @DisplayName("should throw UNAUTHORIZED when user is not the investment owner")
        void shouldThrowWhenNotOwner() {
            UUID otherUserId = UUID.randomUUID();

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));

            assertThatThrownBy(() -> investmentUseCase.cancelInvestment(investmentId, otherUserId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("not authorized");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when investment not found")
        void shouldThrowWhenInvestmentNotFound() {
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> investmentUseCase.cancelInvestment(investmentId, investorId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("should allow cancel when status is COOLING_OFF")
        void shouldAllowCancelForCoolingOffStatus() {
            existingInvestment.setStatus(InvestmentStatus.COOLING_OFF);

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));
            when(investmentRepository.save(any(Investment.class))).thenAnswer(inv -> inv.getArgument(0));
            when(campaignRepository.findByIdAndDeletedFalse(campaignId)).thenReturn(Optional.of(campaign));
            when(campaignRepository.updateRaisedAmount(any(), any(), anyLong(), anyLong())).thenReturn(1);
            when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

            InvestmentResponse response = investmentUseCase.cancelInvestment(investmentId, investorId);

            assertThat(response.getStatus()).isEqualTo("CANCELLED");
        }
    }

    @Nested
    @DisplayName("completeInvestment")
    class CompleteInvestment {

        private UUID investmentId;
        private Investment existingInvestment;

        @BeforeEach
        void setUp() {
            investmentId = UUID.randomUUID();
            existingInvestment = Investment.builder()
                    .investorId(investorId)
                    .campaignId(campaignId)
                    .amount(new BigDecimal("10000"))
                    .shares(100)
                    .sharePrice(new BigDecimal("100"))
                    .status(InvestmentStatus.PENDING)
                    .paymentMethod(PaymentMethod.MPESA)
                    .build();
            existingInvestment.setId(investmentId);
        }

        @Test
        @DisplayName("should complete investment from PENDING status")
        void shouldCompleteFromPending() {
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));
            when(investmentRepository.save(any(Investment.class))).thenAnswer(inv -> inv.getArgument(0));

            InvestmentResponse response = investmentUseCase.completeInvestment(investmentId);

            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            verify(investmentRepository).save(investmentCaptor.capture());
            assertThat(investmentCaptor.getValue().getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("should complete investment from PAYMENT_INITIATED status")
        void shouldCompleteFromPaymentInitiated() {
            existingInvestment.setStatus(InvestmentStatus.PAYMENT_INITIATED);

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));
            when(investmentRepository.save(any(Investment.class))).thenAnswer(inv -> inv.getArgument(0));

            InvestmentResponse response = investmentUseCase.completeInvestment(investmentId);

            assertThat(response.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should complete investment from COOLING_OFF status")
        void shouldCompleteFromCoolingOff() {
            existingInvestment.setStatus(InvestmentStatus.COOLING_OFF);

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));
            when(investmentRepository.save(any(Investment.class))).thenAnswer(inv -> inv.getArgument(0));

            InvestmentResponse response = investmentUseCase.completeInvestment(investmentId);

            assertThat(response.getStatus()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("should throw INVALID_STATUS when completing a CANCELLED investment")
        void shouldThrowWhenCompletingCancelled() {
            existingInvestment.setStatus(InvestmentStatus.CANCELLED);

            when(investmentRepository.findById(investmentId)).thenReturn(Optional.of(existingInvestment));

            assertThatThrownBy(() -> investmentUseCase.completeInvestment(investmentId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cannot be completed from status");
        }

        @Test
        @DisplayName("should throw ResourceNotFoundException when investment not found")
        void shouldThrowWhenNotFound() {
            when(investmentRepository.findById(investmentId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> investmentUseCase.completeInvestment(investmentId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
