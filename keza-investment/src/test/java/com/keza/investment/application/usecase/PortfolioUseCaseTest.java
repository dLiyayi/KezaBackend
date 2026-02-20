package com.keza.investment.application.usecase;

import com.keza.campaign.domain.model.Campaign;
import com.keza.campaign.domain.port.out.CampaignRepository;
import com.keza.common.enums.CampaignStatus;
import com.keza.common.enums.InvestmentStatus;
import com.keza.common.enums.PaymentMethod;
import com.keza.investment.application.dto.PortfolioResponse;
import com.keza.investment.domain.model.Investment;
import com.keza.investment.domain.port.out.InvestmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioUseCase")
class PortfolioUseCaseTest {

    @Mock
    private InvestmentRepository investmentRepository;
    @Mock
    private CampaignRepository campaignRepository;

    @InjectMocks
    private PortfolioUseCase portfolioUseCase;

    private UUID userId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
    }

    private Investment buildInvestment(UUID campaignId, InvestmentStatus status, BigDecimal amount) {
        Investment inv = Investment.builder()
                .investorId(userId)
                .campaignId(campaignId)
                .amount(amount)
                .shares(amount.divide(new BigDecimal("100"), 0, RoundingMode.FLOOR).longValue())
                .sharePrice(new BigDecimal("100"))
                .status(status)
                .paymentMethod(PaymentMethod.MPESA)
                .build();
        inv.setId(UUID.randomUUID());
        return inv;
    }

    private Campaign buildCampaign(UUID id, String industry) {
        Campaign c = Campaign.builder()
                .issuerId(UUID.randomUUID())
                .title("Campaign " + industry)
                .status(CampaignStatus.LIVE)
                .targetAmount(new BigDecimal("1000000"))
                .raisedAmount(BigDecimal.ZERO)
                .sharePrice(new BigDecimal("100"))
                .industry(industry)
                .build();
        c.setId(id);
        return c;
    }

    @Nested
    @DisplayName("getPortfolio")
    class GetPortfolio {

        @Test
        @DisplayName("should return empty portfolio when user has no investments")
        void shouldReturnEmptyPortfolio() {
            Page<Investment> emptyPage = new PageImpl<>(Collections.emptyList());
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(emptyPage);

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getTotalInvested()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(response.getActiveInvestments()).isZero();
            assertThat(response.getSectorDistribution()).isEmpty();
            assertThat(response.getInvestments()).isEmpty();
        }

        @Test
        @DisplayName("should calculate totalInvested from COMPLETED, COOLING_OFF and PENDING investments only")
        void shouldCalculateTotalInvestedCorrectly() {
            UUID c1 = UUID.randomUUID();
            UUID c2 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.COMPLETED, new BigDecimal("5000")),
                    buildInvestment(c1, InvestmentStatus.PENDING, new BigDecimal("3000")),
                    buildInvestment(c2, InvestmentStatus.COOLING_OFF, new BigDecimal("2000")),
                    buildInvestment(c2, InvestmentStatus.CANCELLED, new BigDecimal("4000")),
                    buildInvestment(c2, InvestmentStatus.REFUNDED, new BigDecimal("1000"))
            );

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);
            when(campaignRepository.findAllById(any())).thenReturn(List.of(
                    buildCampaign(c1, "FinTech"),
                    buildCampaign(c2, "AgriTech")
            ));

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            // totalInvested = 5000 + 3000 + 2000 = 10000 (excludes CANCELLED and REFUNDED)
            assertThat(response.getTotalInvested()).isEqualByComparingTo(new BigDecimal("10000"));
        }

        @Test
        @DisplayName("should count only COMPLETED and COOLING_OFF as active investments")
        void shouldCountActiveInvestments() {
            UUID c1 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.COMPLETED, new BigDecimal("5000")),
                    buildInvestment(c1, InvestmentStatus.COOLING_OFF, new BigDecimal("3000")),
                    buildInvestment(c1, InvestmentStatus.PENDING, new BigDecimal("2000")),
                    buildInvestment(c1, InvestmentStatus.CANCELLED, new BigDecimal("1000"))
            );

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);
            when(campaignRepository.findAllById(any())).thenReturn(List.of(buildCampaign(c1, "FinTech")));

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getActiveInvestments()).isEqualTo(2); // COMPLETED + COOLING_OFF
        }

        @Test
        @DisplayName("should compute sector distribution percentages from active investments")
        void shouldComputeSectorDistribution() {
            UUID c1 = UUID.randomUUID();
            UUID c2 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.COMPLETED, new BigDecimal("7000")),
                    buildInvestment(c2, InvestmentStatus.COMPLETED, new BigDecimal("3000"))
            );

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);
            when(campaignRepository.findAllById(any())).thenReturn(List.of(
                    buildCampaign(c1, "FinTech"),
                    buildCampaign(c2, "AgriTech")
            ));

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getSectorDistribution())
                    .containsEntry("FinTech", new BigDecimal("70.00"))
                    .containsEntry("AgriTech", new BigDecimal("30.00"));
        }

        @Test
        @DisplayName("should use 'Other' when campaign industry is null")
        void shouldUseOtherForNullIndustry() {
            UUID c1 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.COMPLETED, new BigDecimal("5000"))
            );

            Campaign campaignNoIndustry = buildCampaign(c1, null);
            campaignNoIndustry.setIndustry(null);

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);
            when(campaignRepository.findAllById(any())).thenReturn(List.of(campaignNoIndustry));

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getSectorDistribution()).containsKey("Other");
            assertThat(response.getSectorDistribution().get("Other")).isEqualByComparingTo(new BigDecimal("100.00"));
        }

        @Test
        @DisplayName("should return empty sector distribution when only CANCELLED/REFUNDED investments exist")
        void shouldReturnEmptySectorWhenNoActiveInvestments() {
            UUID c1 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.CANCELLED, new BigDecimal("5000")),
                    buildInvestment(c1, InvestmentStatus.REFUNDED, new BigDecimal("3000"))
            );

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getSectorDistribution()).isEmpty();
            assertThat(response.getActiveInvestments()).isZero();
        }

        @Test
        @DisplayName("should map all investments to response DTOs")
        void shouldMapAllInvestmentsToResponses() {
            UUID c1 = UUID.randomUUID();

            List<Investment> investments = List.of(
                    buildInvestment(c1, InvestmentStatus.COMPLETED, new BigDecimal("5000")),
                    buildInvestment(c1, InvestmentStatus.CANCELLED, new BigDecimal("3000"))
            );

            Page<Investment> page = new PageImpl<>(investments);
            when(investmentRepository.findByInvestorIdOrderByCreatedAtDesc(eq(userId), any(Pageable.class)))
                    .thenReturn(page);
            when(campaignRepository.findAllById(any())).thenReturn(List.of(buildCampaign(c1, "FinTech")));

            PortfolioResponse response = portfolioUseCase.getPortfolio(userId);

            assertThat(response.getInvestments()).hasSize(2);
        }
    }
}
