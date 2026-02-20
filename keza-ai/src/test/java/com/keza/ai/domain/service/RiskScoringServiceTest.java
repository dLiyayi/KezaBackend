package com.keza.ai.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RiskScoringService")
class RiskScoringServiceTest {

    private RiskScoringService riskScoringService;

    @BeforeEach
    void setUp() {
        riskScoringService = new RiskScoringService();
    }

    @Nested
    @DisplayName("calculateRiskScore")
    class CalculateRiskScore {

        @Test
        @DisplayName("should return a valid risk score result for any campaign")
        void shouldReturnValidRiskScoreResult() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result).isNotNull();
            assertThat(result.score()).isBetween(1, 10);
            assertThat(result.riskLevel()).isNotBlank();
            assertThat(result.strengths()).isNotNull().isNotEmpty();
            assertThat(result.risks()).isNotNull().isNotEmpty();
            assertThat(result.recommendation()).isNotBlank();
        }

        @Test
        @DisplayName("should return MEDIUM risk level for placeholder scores of 5.0")
        void shouldReturnMediumRiskForPlaceholderScores() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            // All dimension placeholders return 5.0, weighted average = 5.0, rounded = 5
            assertThat(result.score()).isEqualTo(5);
            assertThat(result.riskLevel()).isEqualTo("MEDIUM");
        }

        @Test
        @DisplayName("should return appropriate recommendation for MEDIUM risk level")
        void shouldReturnMediumRecommendation() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.recommendation())
                    .contains("moderate risk profile")
                    .contains("Enhanced due diligence");
        }

        @Test
        @DisplayName("should identify no standout strengths when all scores are moderate")
        void shouldIdentifyNoStandoutStrengths() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            // All placeholders return 5.0, which is neither <= 3.0 (strength) nor >= 7.0 (risk)
            assertThat(result.strengths())
                    .hasSize(1)
                    .first()
                    .asString()
                    .contains("No standout strengths");
        }

        @Test
        @DisplayName("should identify no critical risks when all scores are moderate")
        void shouldIdentifyNoCriticalRisks() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.risks())
                    .hasSize(1)
                    .first()
                    .asString()
                    .contains("No critical risks");
        }

        @Test
        @DisplayName("should produce consistent results for the same campaign")
        void shouldProduceConsistentResults() {
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result1 = riskScoringService.calculateRiskScore(campaignId);
            RiskScoringService.RiskScoreResult result2 = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result1.score()).isEqualTo(result2.score());
            assertThat(result1.riskLevel()).isEqualTo(result2.riskLevel());
            assertThat(result1.recommendation()).isEqualTo(result2.recommendation());
        }
    }

    @Nested
    @DisplayName("risk level determination")
    class RiskLevelDetermination {

        @Test
        @DisplayName("should clamp score to range 1-10")
        void shouldClampScoreToValidRange() {
            // With placeholder scores of 5.0, the score will always be 5
            // This test verifies the score is within bounds
            UUID campaignId = UUID.randomUUID();

            RiskScoringService.RiskScoreResult result = riskScoringService.calculateRiskScore(campaignId);

            assertThat(result.score()).isGreaterThanOrEqualTo(1);
            assertThat(result.score()).isLessThanOrEqualTo(10);
        }

        @Test
        @DisplayName("risk level should match score range for placeholder data")
        void riskLevelShouldMatchScoreRange() {
            UUID campaignId = UUID.randomUUID();

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
