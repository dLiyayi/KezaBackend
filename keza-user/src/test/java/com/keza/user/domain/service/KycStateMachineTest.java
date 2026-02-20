package com.keza.user.domain.service;

import com.keza.common.enums.DocumentType;
import com.keza.common.exception.BusinessRuleException;
import com.keza.user.domain.model.KycDocumentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KycStateMachine")
class KycStateMachineTest {

    private KycStateMachine stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = new KycStateMachine();
    }

    @Nested
    @DisplayName("validateTransition - valid transitions")
    class ValidTransitions {

        static Stream<Arguments> validTransitionProvider() {
            return Stream.of(
                    Arguments.of(KycDocumentStatus.PENDING, KycDocumentStatus.IN_REVIEW),
                    Arguments.of(KycDocumentStatus.PENDING, KycDocumentStatus.REJECTED),
                    Arguments.of(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.APPROVED),
                    Arguments.of(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.REJECTED),
                    Arguments.of(KycDocumentStatus.REJECTED, KycDocumentStatus.PENDING)
            );
        }

        @ParameterizedTest(name = "{0} -> {1} should be allowed")
        @MethodSource("validTransitionProvider")
        @DisplayName("should allow valid transition")
        void shouldAllowValidTransition(KycDocumentStatus from, KycDocumentStatus to) {
            assertThatCode(() -> stateMachine.validateTransition(from, to))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("validateTransition - invalid transitions")
    class InvalidTransitions {

        static Stream<Arguments> invalidTransitionProvider() {
            return Stream.of(
                    // PENDING cannot go directly to APPROVED
                    Arguments.of(KycDocumentStatus.PENDING, KycDocumentStatus.APPROVED),
                    // IN_REVIEW cannot go back to PENDING
                    Arguments.of(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.PENDING),
                    // APPROVED is a terminal state - no transitions out
                    Arguments.of(KycDocumentStatus.APPROVED, KycDocumentStatus.PENDING),
                    Arguments.of(KycDocumentStatus.APPROVED, KycDocumentStatus.IN_REVIEW),
                    Arguments.of(KycDocumentStatus.APPROVED, KycDocumentStatus.REJECTED),
                    // REJECTED cannot go to IN_REVIEW or APPROVED directly
                    Arguments.of(KycDocumentStatus.REJECTED, KycDocumentStatus.IN_REVIEW),
                    Arguments.of(KycDocumentStatus.REJECTED, KycDocumentStatus.APPROVED),
                    // Same-state transitions are not allowed
                    Arguments.of(KycDocumentStatus.PENDING, KycDocumentStatus.PENDING),
                    Arguments.of(KycDocumentStatus.IN_REVIEW, KycDocumentStatus.IN_REVIEW),
                    Arguments.of(KycDocumentStatus.APPROVED, KycDocumentStatus.APPROVED),
                    Arguments.of(KycDocumentStatus.REJECTED, KycDocumentStatus.REJECTED)
            );
        }

        @ParameterizedTest(name = "{0} -> {1} should be rejected")
        @MethodSource("invalidTransitionProvider")
        @DisplayName("should throw BusinessRuleException for invalid transition")
        void shouldThrowForInvalidTransition(KycDocumentStatus from, KycDocumentStatus to) {
            assertThatThrownBy(() -> stateMachine.validateTransition(from, to))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition KYC document from " + from + " to " + to);
        }
    }

    @Nested
    @DisplayName("getRequiredDocumentTypes")
    class GetRequiredDocumentTypes {

        @Test
        @DisplayName("should return NATIONAL_ID and SELFIE as required document types")
        void shouldReturnRequiredTypes() {
            Set<DocumentType> requiredTypes = stateMachine.getRequiredDocumentTypes();

            assertThat(requiredTypes).containsExactlyInAnyOrder(
                    DocumentType.NATIONAL_ID,
                    DocumentType.SELFIE
            );
        }

        @Test
        @DisplayName("should return exactly 2 required document types")
        void shouldReturnExactlyTwoTypes() {
            assertThat(stateMachine.getRequiredDocumentTypes()).hasSize(2);
        }
    }
}
