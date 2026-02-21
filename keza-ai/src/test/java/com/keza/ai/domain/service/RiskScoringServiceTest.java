package com.keza.ai.domain.service;

import com.keza.ai.domain.port.out.RiskDataPort;
import com.keza.ai.domain.port.out.RiskDataPort.CampaignRiskData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DisplayName("RiskScoringService")
@ExtendWith(MockitoExtension.class)
class RiskScoringServiceTest {

    @Mock
    private RiskDataPort riskDataPort;

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        // No ChatModel (LLM disabled) -- 100% rule-based scoring
        riskScoringService = new RiskScoringService(riskDataPort, null);
    }

    // ---- Test data builders ----

    /**
     * Creates a well-documented campaign: all sections filled, KYC approved, track record.
     * Expected: low risk score.
     */
    private CampaignRiskData wellDocumentedCampaign(UUID id) {
        return new CampaignRiskData(
                id,
                "Nairobi Solar Solutions",
                "A".repeat(600),            // Long description (>500 chars)
                "Renewable Energy",
                "SolarTech Kenya Ltd",
                "CPR-2024-001234",           // Company registration
                "https://solartech.co.ke",   // Company website
                "123 Ngong Rd, Nairobi",     // Company address
                new BigDecimal("5000000"),   // KES 5M target (reasonable)
                new BigDecimal("2000000"),   // KES 2M raised (40% progress)
                55,                          // 55 investors
                6,                           // Wizard completed
                true,                        // Financial projections
                true,                        // Risk factors
                true,                        // Use of funds
                true,                        // Team members
                true,                        // Pitch video
                5,                           // 5 media items
                3,                           // 3 updates
                true,                        // KYC approved
                2                            // 2 previous campaigns
        );
    }

    /**
     * Creates a bare-minimum campaign: almost nothing filled in.
     * Expected: high risk score.
     */
    private CampaignRiskData bareMinimumCampaign(UUID id) {
        return new CampaignRiskData(
                id,
                "My Campaign",
                "Short desc",               // Very short description
                null,                        // No industry
                null,                        // No company name
                null,                        // No registration
                null,                        // No website
                null,                        // No address
                new BigDecimal("50000"),     // Below reasonable range
                BigDecimal.ZERO,             // Nothing raised
                0,                           // No investors
                1,                           // Wizard step 1
                false,                       // No financial projections
                false,                       // No risk factors
                false,                       // No use of funds
                false,                       // No team members
                false,                       // No pitch video
                0,                           // No media
                0,                           // No updates
                false,                       // KYC not approved
                0                            // No previous campaigns
        );
    }

    /**
     * Creates a moderately complete campaign for specific dimension testing.
     */
    private CampaignRiskData moderateCampaign(UUID id) {
        return new CampaignRiskData(
                id,
                "AgriTech Kenya",
                "A".repeat(200),             // Moderate description
                "Agriculture",
                "AgriTech Ltd",
                "CPR-2024-005678",
                null,                        // No website
                "456 Moi Ave, Mombasa",
                new BigDecimal("2000000"),   // Reasonable target
                new BigDecimal("300000"),    // 15% progress
                8,                           // 8 investors
                4,                           // Mid-wizard
                true,                        // Has financials
                false,                       // No risk factors
                true,                        // Has use of funds
                false,                       // No team
                false,                       // No pitch video
                1,                           // 1 media
                0,                           // No updates
                true,                        // KYC approved
                0                            // First campaign
        );
    }

    @Nested
    @DisplayName("calculateRiskScore - well-documented campaign")
    class WellDocumentedCampaignScoring {

        @Test
        @DisplayName("should return a low risk score for a well-documented campaign")
        void shouldReturnLowRiskScore() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(wellDocumentedCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result).isNotNull();
            assertThat(result.score()).isLessThanOrEqualTo(3);
            assertThat(result.riskLevel()).isIn("VERY_LOW", "LOW");
        }

        @Test
        @DisplayName("should identify multiple strengths for well-documented campaign")
        void shouldIdentifyMultipleStrengths() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(wellDocumentedCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.strengths()).hasSizeGreaterThan(1);
            assertThat(result.strengths()).noneMatch(s -> s.contains("No standout strengths"));
        }
    }

    @Nested
    @DisplayName("calculateRiskScore - bare-minimum campaign")
    class BareMinimumCampaignScoring {

        @Test
        @DisplayName("should return a high risk score for a bare-minimum campaign")
        void shouldReturnHighRiskScore() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(bareMinimumCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result).isNotNull();
            assertThat(result.score()).isGreaterThanOrEqualTo(8);
            assertThat(result.riskLevel()).isIn("HIGH", "VERY_HIGH");
        }

        @Test
        @DisplayName("should identify multiple risk concerns for bare-minimum campaign")
        void shouldIdentifyMultipleRisks() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(bareMinimumCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.risks()).hasSizeGreaterThan(1);
            assertThat(result.risks()).noneMatch(s -> s.contains("No critical risks"));
        }
    }

    @Nested
    @DisplayName("calculateRiskScore - general behavior")
    class GeneralBehavior {

        @Test
        @DisplayName("should return a valid risk score result for any campaign")
        void shouldReturnValidRiskScoreResult() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(moderateCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result).isNotNull();
            assertThat(result.score()).isBetween(1, 10);
            assertThat(result.riskLevel()).isNotBlank();
            assertThat(result.strengths()).isNotNull().isNotEmpty();
            assertThat(result.risks()).isNotNull().isNotEmpty();
            assertThat(result.recommendation()).isNotBlank();
        }

        @Test
        @DisplayName("should produce consistent results for the same campaign data")
        void shouldProduceConsistentResults() {
            UUID campaignId = UUID.randomUUID();
            CampaignRiskData data = moderateCampaign(campaignId);
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(data);

            RiskScoringService.RiskScoreResult result1 = riskScoringService.calculateRiskScore(campaignId);
            RiskScoringService.RiskScoreResult result2 = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result1.score()).isEqualTo(result2.score());
            assertThat(result1.riskLevel()).isEqualTo(result2.riskLevel());
            assertThat(result1.recommendation()).isEqualTo(result2.recommendation());
        }

        @Test
        @DisplayName("should clamp score to range 1-10")
        void shouldClampScoreToValidRange() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(wellDocumentedCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.score()).isGreaterThanOrEqualTo(1);
            assertThat(result.score()).isLessThanOrEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Individual scoring dimensions")
    class ScoringDimensions {

        @Test
        @DisplayName("scoreCampaignCompleteness should return low score for complete campaign")
        void campaignCompletenessShouldBeLowForCompleteCampaign() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreCampaignCompleteness(data);

            assertThat(score).isLessThanOrEqualTo(2.0);
        }

        @Test
        @DisplayName("scoreCampaignCompleteness should return high score for incomplete campaign")
        void campaignCompletenessShouldBeHighForIncompleteCampaign() {
            CampaignRiskData data = bareMinimumCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreCampaignCompleteness(data);

            assertThat(score).isGreaterThanOrEqualTo(9.0);
        }

        @Test
        @DisplayName("scoreFounderCredibility should return low score for verified issuer")
        void founderCredibilityShouldBeLowForVerifiedIssuer() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreFounderCredibility(data);

            assertThat(score).isLessThanOrEqualTo(3.0);
        }

        @Test
        @DisplayName("scoreFounderCredibility should return high score for unverified issuer")
        void founderCredibilityShouldBeHighForUnverifiedIssuer() {
            CampaignRiskData data = bareMinimumCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreFounderCredibility(data);

            assertThat(score).isEqualTo(10.0);
        }

        @Test
        @DisplayName("scoreFinancialHealth should return low score for well-funded campaign")
        void financialHealthShouldBeLowForWellFundedCampaign() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreFinancialHealth(data);

            assertThat(score).isLessThanOrEqualTo(2.0);
        }

        @Test
        @DisplayName("scoreFinancialHealth should return high score for campaign with no financials")
        void financialHealthShouldBeHighForCampaignWithNoFinancials() {
            CampaignRiskData data = bareMinimumCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreFinancialHealth(data);

            assertThat(score).isGreaterThanOrEqualTo(8.0);
        }

        @Test
        @DisplayName("scoreMarketPotential should return low score for campaign with strong traction")
        void marketPotentialShouldBeLowForStrongTraction() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreMarketPotential(data);

            assertThat(score).isLessThanOrEqualTo(3.0);
        }

        @Test
        @DisplayName("scoreRegulatoryCompliance should return low score for compliant campaign")
        void regulatoryComplianceShouldBeLowForCompliantCampaign() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreRegulatoryCompliance(data);

            assertThat(score).isLessThanOrEqualTo(3.0);
        }

        @Test
        @DisplayName("scoreRegulatoryCompliance should return high score for non-compliant campaign")
        void regulatoryComplianceShouldBeHighForNonCompliantCampaign() {
            CampaignRiskData data = bareMinimumCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreRegulatoryCompliance(data);

            assertThat(score).isEqualTo(10.0);
        }

        @Test
        @DisplayName("scoreCommunityTrust should return low score for popular campaign")
        void communityTrustShouldBeLowForPopularCampaign() {
            CampaignRiskData data = wellDocumentedCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreCommunityTrust(data);

            assertThat(score).isLessThanOrEqualTo(3.0);
        }

        @Test
        @DisplayName("scoreCommunityTrust should return high score for campaign with no community")
        void communityTrustShouldBeHighForNoCommunity() {
            CampaignRiskData data = bareMinimumCampaign(UUID.randomUUID());

            double score = riskScoringService.scoreCommunityTrust(data);

            assertThat(score).isEqualTo(10.0);
        }
    }

    @Nested
    @DisplayName("LLM integration behavior")
    class LlmIntegration {

        @Test
        @DisplayName("should use 100% rule-based scoring when ChatModel is null")
        void shouldUseRuleBasedWhenChatModelIsNull() {
            // riskScoringService is already created with null ChatModel in setUp()
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(moderateCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            // Should still produce a valid result without LLM
            assertThat(result).isNotNull();
            assertThat(result.score()).isBetween(1, 10);
            // No [AI] prefixed insights should appear
            assertThat(result.strengths()).noneMatch(s -> s.startsWith("[AI]"));
            assertThat(result.risks()).noneMatch(s -> s.startsWith("[AI]"));
        }
    }

    @Nested
    @DisplayName("risk level determination")
    class RiskLevelDetermination {

        @Test
        @DisplayName("risk level should match score range for well-documented campaign")
        void riskLevelShouldMatchScoreRangeForLowRisk() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(wellDocumentedCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            int score = result.score();
            String level = result.riskLevel();

            if (score <= 2) assertThat(level).isEqualTo("VERY_LOW");
            else if (score <= 4) assertThat(level).isEqualTo("LOW");
            else if (score <= 6) assertThat(level).isEqualTo("MEDIUM");
            else if (score <= 8) assertThat(level).isEqualTo("HIGH");
            else assertThat(level).isEqualTo("VERY_HIGH");
        }

        @Test
        @DisplayName("risk level should match score range for bare-minimum campaign")
        void riskLevelShouldMatchScoreRangeForHighRisk() {
            UUID campaignId = UUID.randomUUID();
            when(riskDataPort.getCampaignData(campaignId)).thenReturn(bareMinimumCampaign(campaignId));

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            int score = result.score();
            String level = result.riskLevel();

            if (score <= 2) assertThat(level).isEqualTo("VERY_LOW");
            else if (score <= 4) assertThat(level).isEqualTo("LOW");
            else if (score <= 6) assertThat(level).isEqualTo("MEDIUM");
            else if (score <= 8) assertThat(level).isEqualTo("HIGH");
            else assertThat(level).isEqualTo("VERY_HIGH");
        }
    }

    @Nested
    @DisplayName("RiskScoreResult record")
    class RiskScoreResultRecord {

        @Test
        @DisplayName("should expose all fields via accessors")
        void shouldExposeAllFields() {
            var result = new RiskScoringService.RiskScoreResult(
                    7, "HIGH",
                    java.util.List.of("Strong financials"),
                    java.util.List.of("Weak compliance"),
                    "Thorough review recommended"
            );

            assertThat(result.score()).isEqualTo(7);
            assertThat(result.riskLevel()).isEqualTo("HIGH");
            assertThat(result.strengths()).containsExactly("Strong financials");
            assertThat(result.risks()).containsExactly("Weak compliance");
            assertThat(result.recommendation()).isEqualTo("Thorough review recommended");
        }

        @Test
        @DisplayName("should support equality based on all fields")
        void shouldSupportEquality() {
            var list1 = java.util.List.of("A");
            var list2 = java.util.List.of("B");

            var r1 = new RiskScoringService.RiskScoreResult(5, "MEDIUM", list1, list2, "rec");
            var r2 = new RiskScoringService.RiskScoreResult(5, "MEDIUM", list1, list2, "rec");

            assertThat(r1).isEqualTo(r2);
            assertThat(r1.hashCode()).isEqualTo(r2.hashCode());
        }
    }
}
