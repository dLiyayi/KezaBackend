package com.keza.notification.adapter.out.external;

import com.africastalking.AfricasTalking;
import com.africastalking.SmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AfricasTalkingSmsSender")
class AfricasTalkingSmsSenderTest {

    @Mock
    private SmsService smsService;

    private AfricasTalkingSmsSender smsSender;

    @Nested
    @DisplayName("send with sender ID")
    class WithSenderId {

        @BeforeEach
        void setUp() {
            try (MockedStatic<AfricasTalking> mockedAt = mockStatic(AfricasTalking.class)) {
                mockedAt.when(() -> AfricasTalking.initialize(anyString(), anyString())).thenAnswer(inv -> null);
                mockedAt.when(() -> AfricasTalking.getService(AfricasTalking.SERVICE_SMS)).thenReturn(smsService);
                smsSender = new AfricasTalkingSmsSender("test-api-key", "sandbox", "KEZA");
            }
        }

        @Test
        @DisplayName("should send SMS with sender ID when configured")
        void shouldSendSmsWithSenderId() throws Exception {
            when(smsService.send(anyString(), anyString(), any(String[].class), eq(false)))
                    .thenReturn(Collections.emptyList());

            smsSender.send("+254712345678", "Your OTP is 123456");

            verify(smsService).send(
                    eq("Your OTP is 123456"),
                    eq("KEZA"),
                    eq(new String[]{"+254712345678"}),
                    eq(false)
            );
        }

        @Test
        @DisplayName("should throw RuntimeException when SMS sending fails")
        void shouldThrowWhenSmsFails() throws Exception {
            when(smsService.send(anyString(), anyString(), any(String[].class), eq(false)))
                    .thenThrow(new IOException("Network error"));

            assertThatThrownBy(() -> smsSender.send("+254712345678", "Test message"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Failed to send SMS via Africa's Talking")
                    .hasMessageContaining("+254712345678")
                    .hasCauseInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("send without sender ID")
    class WithoutSenderId {

        @BeforeEach
        void setUp() {
            try (MockedStatic<AfricasTalking> mockedAt = mockStatic(AfricasTalking.class)) {
                mockedAt.when(() -> AfricasTalking.initialize(anyString(), anyString())).thenAnswer(inv -> null);
                mockedAt.when(() -> AfricasTalking.getService(AfricasTalking.SERVICE_SMS)).thenReturn(smsService);
                smsSender = new AfricasTalkingSmsSender("test-api-key", "sandbox", "");
            }
        }

        @Test
        @DisplayName("should send SMS without sender ID when not configured")
        void shouldSendSmsWithoutSenderId() throws Exception {
            when(smsService.send(anyString(), any(String[].class), eq(false)))
                    .thenReturn(Collections.emptyList());

            smsSender.send("+254712345678", "Your OTP is 123456");

            verify(smsService).send(
                    eq("Your OTP is 123456"),
                    eq(new String[]{"+254712345678"}),
                    eq(false)
            );
            verify(smsService, never()).send(anyString(), anyString(), any(String[].class), anyBoolean());
        }
    }
}
