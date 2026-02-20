package com.keza.payment.domain.service;

import com.keza.common.enums.PaymentMethod;
import com.keza.common.exception.BusinessRuleException;
import com.keza.payment.domain.port.out.PaymentGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("PaymentRouter")
class PaymentRouterTest {

    private PaymentGateway mpesaGateway;
    private PaymentGateway cardGateway;
    private PaymentGateway kcbGateway;
    private PaymentRouter paymentRouter;

    @BeforeEach
    void setUp() {
        mpesaGateway = mock(PaymentGateway.class);
        cardGateway = mock(PaymentGateway.class);
        kcbGateway = mock(PaymentGateway.class);

        when(mpesaGateway.getName()).thenReturn("mpesa");
        when(cardGateway.getName()).thenReturn("card");
        when(kcbGateway.getName()).thenReturn("kcb");

        paymentRouter = new PaymentRouter(List.of(mpesaGateway, cardGateway, kcbGateway));
    }

    @Nested
    @DisplayName("route")
    class Route {

        @Test
        @DisplayName("should route MPESA to mpesa gateway")
        void shouldRouteMpesa() {
            PaymentGateway result = paymentRouter.route(PaymentMethod.MPESA);

            assertThat(result).isSameAs(mpesaGateway);
        }

        @Test
        @DisplayName("should route CARD to card gateway")
        void shouldRouteCard() {
            PaymentGateway result = paymentRouter.route(PaymentMethod.CARD);

            assertThat(result).isSameAs(cardGateway);
        }

        @Test
        @DisplayName("should route BANK_TRANSFER to kcb gateway")
        void shouldRouteBankTransfer() {
            PaymentGateway result = paymentRouter.route(PaymentMethod.BANK_TRANSFER);

            assertThat(result).isSameAs(kcbGateway);
        }

        @Test
        @DisplayName("should route KCB_ESCROW to kcb gateway")
        void shouldRouteKcbEscrow() {
            PaymentGateway result = paymentRouter.route(PaymentMethod.KCB_ESCROW);

            assertThat(result).isSameAs(kcbGateway);
        }

        @Test
        @DisplayName("should throw UNSUPPORTED_PAYMENT_METHOD when gateway not registered")
        void shouldThrowWhenGatewayNotRegistered() {
            // Create a router with only mpesa gateway registered
            PaymentRouter partialRouter = new PaymentRouter(List.of(mpesaGateway));

            assertThatThrownBy(() -> partialRouter.route(PaymentMethod.CARD))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No gateway registered")
                    .hasMessageContaining("CARD")
                    .hasMessageContaining("card");
        }
    }

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should initialize gateway map from list")
        void shouldInitializeGatewayMap() {
            // Verifying by routing each method successfully
            assertThat(paymentRouter.route(PaymentMethod.MPESA)).isNotNull();
            assertThat(paymentRouter.route(PaymentMethod.CARD)).isNotNull();
            assertThat(paymentRouter.route(PaymentMethod.BANK_TRANSFER)).isNotNull();
            assertThat(paymentRouter.route(PaymentMethod.KCB_ESCROW)).isNotNull();
        }

        @Test
        @DisplayName("should handle empty gateway list gracefully")
        void shouldHandleEmptyGatewayList() {
            PaymentRouter emptyRouter = new PaymentRouter(List.of());

            assertThatThrownBy(() -> emptyRouter.route(PaymentMethod.MPESA))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("No gateway registered");
        }
    }
}
