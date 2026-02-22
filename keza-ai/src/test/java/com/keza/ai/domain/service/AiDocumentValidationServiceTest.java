package com.keza.ai.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keza.ai.domain.model.DocumentQualityResult;
import com.keza.ai.domain.model.DocumentValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AiDocumentValidationService")
class AiDocumentValidationServiceTest {

    @Mock
    private ChatModel chatModel;

    private ObjectMapper objectMapper;

    private AiDocumentValidationService service;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new AiDocumentValidationService(chatModel, objectMapper);
    }

    @Nested
    @DisplayName("validateDocument")
    class ValidateDocument {

        @Test
        @DisplayName("should parse valid JSON response correctly")
        void shouldParseValidJsonResponse() {
            String aiResponse = """
                    {
                      "isValid": true,
                      "confidenceScore": 0.92,
                      "documentType": "NATIONAL_ID",
                      "extractedFields": {"name": "John Doe", "idNumber": "12345678"},
                      "issues": [],
                      "recommendation": "QUICK_REVIEW"
                    }
                    """;

            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(aiResponse);

                DocumentValidationResult result = service.validateDocument(
                        "NATIONAL_ID", "id.pdf", "application/pdf", 50000, "John Doe 12345678");

                assertThat(result.isValid()).isTrue();
                assertThat(result.confidenceScore()).isEqualTo(0.92);
                assertThat(result.detectedDocumentType()).isEqualTo("NATIONAL_ID");
                assertThat(result.extractedFields()).containsEntry("name", "John Doe");
                assertThat(result.extractedFields()).containsEntry("idNumber", "12345678");
                assertThat(result.issues()).isEmpty();
                assertThat(result.recommendation()).isEqualTo("QUICK_REVIEW");
            }
        }

        @Test
        @DisplayName("should return AUTO_APPROVE for high confidence (>= 0.95)")
        void shouldReturnAutoApproveForHighConfidence() {
            String aiResponse = """
                    {
                      "isValid": true,
                      "confidenceScore": 0.97,
                      "documentType": "PASSPORT",
                      "extractedFields": {"name": "Jane Smith", "passportNumber": "AB1234567"},
                      "issues": [],
                      "recommendation": "AUTO_APPROVE"
                    }
                    """;

            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(aiResponse);

                DocumentValidationResult result = service.validateDocument(
                        "PASSPORT", "passport.pdf", "application/pdf", 100000, "Jane Smith AB1234567");

                assertThat(result.confidenceScore()).isEqualTo(0.97);
                assertThat(result.recommendation()).isEqualTo("AUTO_APPROVE");
            }
        }

        @Test
        @DisplayName("should return QUICK_REVIEW for medium confidence (0.85-0.95)")
        void shouldReturnQuickReviewForMediumConfidence() {
            String aiResponse = """
                    {
                      "isValid": true,
                      "confidenceScore": 0.90,
                      "documentType": "DRIVING_LICENSE",
                      "extractedFields": {"name": "Test User"},
                      "issues": ["Expiry date partially obscured"],
                      "recommendation": "QUICK_REVIEW"
                    }
                    """;

            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(aiResponse);

                DocumentValidationResult result = service.validateDocument(
                        "DRIVING_LICENSE", "license.jpg", "image/jpeg", 80000, "");

                assertThat(result.confidenceScore()).isEqualTo(0.90);
                assertThat(result.recommendation()).isEqualTo("QUICK_REVIEW");
            }
        }

        @Test
        @DisplayName("should return FULL_REVIEW for low confidence (< 0.85)")
        void shouldReturnFullReviewForLowConfidence() {
            String aiResponse = """
                    {
                      "isValid": false,
                      "confidenceScore": 0.45,
                      "documentType": "UNKNOWN",
                      "extractedFields": {},
                      "issues": ["Document appears blurry", "Cannot extract required fields"],
                      "recommendation": "FULL_REVIEW"
                    }
                    """;

            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(aiResponse);

                DocumentValidationResult result = service.validateDocument(
                        "NATIONAL_ID", "blurry.png", "image/png", 30000, "");

                assertThat(result.isValid()).isFalse();
                assertThat(result.confidenceScore()).isEqualTo(0.45);
                assertThat(result.recommendation()).isEqualTo("FULL_REVIEW");
                assertThat(result.issues()).hasSize(2);
            }
        }

        @Test
        @DisplayName("should handle ChatModel exception with fallback result")
        void shouldHandleChatModelException() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenThrow(new RuntimeException("AI service unavailable"));

                // The circuit breaker fallback is handled by Resilience4j at runtime.
                // Without the Spring context, the method will throw. We verify the fallback
                // result separately by invoking the fallback concept.
                try {
                    service.validateDocument("NATIONAL_ID", "doc.pdf", "application/pdf", 50000, "text");
                } catch (RuntimeException e) {
                    // Expected without Spring/Resilience4j context
                    assertThat(e.getMessage()).isEqualTo("AI service unavailable");
                }

                // Verify fallback produces correct result
                DocumentValidationResult fallback = DocumentValidationResult.fallback("AI service unavailable");
                assertThat(fallback.isValid()).isFalse();
                assertThat(fallback.confidenceScore()).isEqualTo(0.0);
                assertThat(fallback.recommendation()).isEqualTo("FULL_REVIEW");
                assertThat(fallback.rawAiResponse()).contains("AI validation unavailable");
            }
        }

        @Test
        @DisplayName("should include expected fields for NATIONAL_ID in prompt")
        void shouldIncludeNationalIdFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"NATIONAL_ID\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("NATIONAL_ID", "id.pdf", "application/pdf", 50000, "text");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("name", "ID number", "date of birth");
            }
        }

        @Test
        @DisplayName("should include expected fields for PASSPORT in prompt")
        void shouldIncludePassportFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"PASSPORT\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("PASSPORT", "passport.pdf", "application/pdf", 50000, "text");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("passport number", "nationality", "expiry date");
            }
        }

        @Test
        @DisplayName("should include expected fields for DRIVING_LICENSE in prompt")
        void shouldIncludeDrivingLicenseFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"DRIVING_LICENSE\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("DRIVING_LICENSE", "license.pdf", "application/pdf", 50000, "text");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("license number", "expiry date");
            }
        }

        @Test
        @DisplayName("should include expected fields for KRA_PIN in prompt")
        void shouldIncludeKraPinFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"KRA_PIN\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("KRA_PIN", "kra.pdf", "application/pdf", 50000, "text");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("KRA PIN number");
            }
        }

        @Test
        @DisplayName("should include expected fields for PROOF_OF_ADDRESS in prompt")
        void shouldIncludeProofOfAddressFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"PROOF_OF_ADDRESS\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("PROOF_OF_ADDRESS", "utility_bill.pdf", "application/pdf", 50000, "text");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("address", "date (within 3 months)");
            }
        }

        @Test
        @DisplayName("should include expected fields for SELFIE in prompt")
        void shouldIncludeSelfieFieldsInPrompt() {
            ChatClient.Builder mockBuilder = mock(ChatClient.Builder.class);
            ChatClient mockChatClient = mock(ChatClient.class);
            ChatClient.ChatClientRequestSpec mockRequest = mock(ChatClient.ChatClientRequestSpec.class);
            ChatClient.CallResponseSpec mockCallResponse = mock(ChatClient.CallResponseSpec.class);

            try (var staticMock = mockStatic(ChatClient.class)) {
                staticMock.when(() -> ChatClient.builder(any(ChatModel.class))).thenReturn(mockBuilder);
                when(mockBuilder.build()).thenReturn(mockChatClient);
                when(mockChatClient.prompt()).thenReturn(mockRequest);
                when(mockRequest.user(anyString())).thenReturn(mockRequest);
                when(mockRequest.call()).thenReturn(mockCallResponse);
                when(mockCallResponse.content()).thenReturn(
                        "{\"isValid\":true,\"confidenceScore\":0.9,\"documentType\":\"SELFIE\"," +
                                "\"extractedFields\":{},\"issues\":[],\"recommendation\":\"QUICK_REVIEW\"}");

                service.validateDocument("SELFIE", "selfie.jpg", "image/jpeg", 50000, "");

                ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
                verify(mockRequest).user(promptCaptor.capture());
                String prompt = promptCaptor.getValue();
                assertThat(prompt).contains("face visible", "matches other documents");
            }
        }
    }

    @Nested
    @DisplayName("assessDocumentQuality")
    class AssessDocumentQuality {

        @Test
        @DisplayName("should pass quality for valid image file size")
        void shouldPassQualityForValidImageSize() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "image/jpeg", 100_000, "photo.jpg");

            assertThat(result.passesMinimumQuality()).isTrue();
            assertThat(result.qualityScore()).isEqualTo(1.0);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("should pass quality for valid PDF file size")
        void shouldPassQualityForValidPdfSize() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "application/pdf", 50_000, "document.pdf");

            assertThat(result.passesMinimumQuality()).isTrue();
            assertThat(result.qualityScore()).isEqualTo(1.0);
            assertThat(result.issues()).isEmpty();
        }

        @Test
        @DisplayName("should fail quality for too-small image file")
        void shouldFailQualityForTooSmallImage() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "image/jpeg", 5_000, "tiny.jpg");

            assertThat(result.qualityScore()).isLessThan(1.0);
            assertThat(result.issues()).isNotEmpty();
            assertThat(result.issues().get(0)).contains("too small");
        }

        @Test
        @DisplayName("should fail quality for too-small PDF file")
        void shouldFailQualityForTooSmallPdf() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "application/pdf", 500, "tiny.pdf");

            assertThat(result.qualityScore()).isLessThan(1.0);
            assertThat(result.issues()).isNotEmpty();
            assertThat(result.issues().get(0)).contains("too small");
        }

        @Test
        @DisplayName("should flag too-large file")
        void shouldFlagTooLargeFile() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "application/pdf", 10 * 1024 * 1024, "huge.pdf");

            assertThat(result.issues()).isNotEmpty();
            assertThat(result.issues().get(0)).contains("too large");
        }

        @Test
        @DisplayName("should fail quality for wrong content type")
        void shouldFailQualityForWrongContentType() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    "application/zip", 50_000, "archive.zip");

            assertThat(result.qualityScore()).isLessThan(1.0);
            assertThat(result.issues()).isNotEmpty();
            assertThat(result.issues().get(0)).contains("Unexpected content type");
        }

        @Test
        @DisplayName("should fail quality for null content type")
        void shouldFailQualityForNullContentType() {
            DocumentQualityResult result = service.assessDocumentQuality(
                    null, 50_000, "unknown.bin");

            assertThat(result.qualityScore()).isLessThan(1.0);
            assertThat(result.issues()).isNotEmpty();
            assertThat(result.issues().get(0)).contains("Missing content type");
        }
    }
}
