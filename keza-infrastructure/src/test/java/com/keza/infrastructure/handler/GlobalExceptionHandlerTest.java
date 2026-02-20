package com.keza.infrastructure.handler;

import com.keza.common.dto.ApiError;
import com.keza.common.dto.ApiResponse;
import com.keza.common.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Nested
    @DisplayName("ResourceNotFoundException")
    class ResourceNotFoundTests {

        @Test
        @DisplayName("should return 404 NOT_FOUND with correct error code")
        void shouldReturn404() {
            ResourceNotFoundException ex = new ResourceNotFoundException("User", UUID.randomUUID());

            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(404);
            assertThat(response.getBody().getError().code()).isEqualTo("RESOURCE_NOT_FOUND");
        }

        @Test
        @DisplayName("should include resource details in message")
        void shouldIncludeResourceDetails() {
            UUID id = UUID.randomUUID();
            ResourceNotFoundException ex = new ResourceNotFoundException("Campaign", id);

            ResponseEntity<ApiResponse<Void>> response = handler.handleNotFound(ex);

            assertThat(response.getBody().getMessage()).contains("Campaign").contains(id.toString());
        }
    }

    @Nested
    @DisplayName("DuplicateResourceException")
    class DuplicateResourceTests {

        @Test
        @DisplayName("should return 409 CONFLICT with DUPLICATE_RESOURCE code")
        void shouldReturn409() {
            DuplicateResourceException ex = new DuplicateResourceException("User", "email", "test@keza.com");

            ResponseEntity<ApiResponse<Void>> response = handler.handleDuplicate(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(409);
            assertThat(response.getBody().getError().code()).isEqualTo("DUPLICATE_RESOURCE");
            assertThat(response.getBody().getMessage()).contains("User").contains("email").contains("test@keza.com");
        }
    }

    @Nested
    @DisplayName("BusinessRuleException")
    class BusinessRuleTests {

        @Test
        @DisplayName("should return 422 UNPROCESSABLE_ENTITY")
        void shouldReturn422() {
            BusinessRuleException ex = new BusinessRuleException("Minimum investment is KES 1000");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessRule(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(422);
            assertThat(response.getBody().getError().code()).isEqualTo("BUSINESS_RULE_VIOLATION");
            assertThat(response.getBody().getMessage()).isEqualTo("Minimum investment is KES 1000");
        }

        @Test
        @DisplayName("should use custom code when provided")
        void shouldUseCustomCode() {
            BusinessRuleException ex = new BusinessRuleException("INVESTMENT_CLOSED", "Campaign is no longer accepting investments");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBusinessRule(ex);

            assertThat(response.getBody().getError().code()).isEqualTo("INVESTMENT_CLOSED");
        }
    }

    @Nested
    @DisplayName("UnauthorizedException")
    class UnauthorizedTests {

        @Test
        @DisplayName("should return 401 UNAUTHORIZED")
        void shouldReturn401() {
            UnauthorizedException ex = new UnauthorizedException("Token expired");

            ResponseEntity<ApiResponse<Void>> response = handler.handleUnauthorized(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(401);
            assertThat(response.getBody().getError().code()).isEqualTo("UNAUTHORIZED");
        }
    }

    @Nested
    @DisplayName("ForbiddenException")
    class ForbiddenTests {

        @Test
        @DisplayName("should return 403 FORBIDDEN")
        void shouldReturn403() {
            ForbiddenException ex = new ForbiddenException("Insufficient permissions");

            ResponseEntity<ApiResponse<Void>> response = handler.handleForbidden(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(403);
            assertThat(response.getBody().getError().code()).isEqualTo("FORBIDDEN");
        }
    }

    @Nested
    @DisplayName("RateLimitException")
    class RateLimitTests {

        @Test
        @DisplayName("should return 429 TOO_MANY_REQUESTS")
        void shouldReturn429() {
            RateLimitException ex = new RateLimitException();

            ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimit(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(429);
            assertThat(response.getBody().getError().code()).isEqualTo("RATE_LIMITED");
        }

        @Test
        @DisplayName("should use custom message when provided")
        void shouldUseCustomMessage() {
            RateLimitException ex = new RateLimitException("API rate limit exceeded");

            ResponseEntity<ApiResponse<Void>> response = handler.handleRateLimit(ex);

            assertThat(response.getBody().getMessage()).isEqualTo("API rate limit exceeded");
        }
    }

    @Nested
    @DisplayName("BadCredentialsException")
    class BadCredentialsTests {

        @Test
        @DisplayName("should return 401 with BAD_CREDENTIALS code")
        void shouldReturn401() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<ApiResponse<Void>> response = handler.handleBadCredentials(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials");
            assertThat(response.getBody().getError().code()).isEqualTo("BAD_CREDENTIALS");
            assertThat(response.getBody().getError().message()).isEqualTo("Invalid email or password");
        }
    }

    @Nested
    @DisplayName("AccessDeniedException")
    class AccessDeniedTests {

        @Test
        @DisplayName("should return 403 with ACCESS_DENIED code")
        void shouldReturn403() {
            AccessDeniedException ex = new AccessDeniedException("Access denied");

            ResponseEntity<ApiResponse<Void>> response = handler.handleAccessDenied(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().code()).isEqualTo("ACCESS_DENIED");
            assertThat(response.getBody().getError().message()).contains("permission");
        }
    }

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class ValidationTests {

        @Test
        @DisplayName("should return 400 with field errors")
        void shouldReturn400WithFieldErrors() {
            BindingResult bindingResult = mock(BindingResult.class);
            FieldError fieldError1 = new FieldError("user", "email", "must not be blank");
            FieldError fieldError2 = new FieldError("user", "name", "size must be between 2 and 100");
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

            ResponseEntity<ApiResponse<Void>> response = handler.handleValidation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("Validation failed");
            assertThat(response.getBody().getError().code()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getError().fieldErrors()).hasSize(2);
            assertThat(response.getBody().getError().fieldErrors().get(0).field()).isEqualTo("email");
            assertThat(response.getBody().getError().fieldErrors().get(1).field()).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("ConstraintViolationException")
    class ConstraintViolationTests {

        @Test
        @DisplayName("should return 400 with constraint violation details")
        @SuppressWarnings("unchecked")
        void shouldReturn400WithViolations() {
            ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
            Path path = mock(Path.class);
            when(path.toString()).thenReturn("phone");
            when(violation.getPropertyPath()).thenReturn(path);
            when(violation.getMessage()).thenReturn("invalid phone number");

            ConstraintViolationException ex = new ConstraintViolationException(Set.of(violation));

            ResponseEntity<ApiResponse<Void>> response = handler.handleConstraintViolation(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().code()).isEqualTo("VALIDATION_ERROR");
            assertThat(response.getBody().getError().fieldErrors()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("MaxUploadSizeExceededException")
    class MaxUploadTests {

        @Test
        @DisplayName("should return 413 PAYLOAD_TOO_LARGE")
        void shouldReturn413() {
            MaxUploadSizeExceededException ex = new MaxUploadSizeExceededException(10485760);

            ResponseEntity<ApiResponse<Void>> response = handler.handleMaxUpload(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PAYLOAD_TOO_LARGE);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().code()).isEqualTo("FILE_TOO_LARGE");
            assertThat(response.getBody().getError().status()).isEqualTo(413);
        }
    }

    @Nested
    @DisplayName("KezaException (generic)")
    class KezaExceptionTests {

        @Test
        @DisplayName("should return 500 INTERNAL_SERVER_ERROR for base KezaException")
        void shouldReturn500() {
            KezaException ex = new KezaException("Something broke");

            ResponseEntity<ApiResponse<Void>> response = handler.handleKeza(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getError().status()).isEqualTo(500);
            assertThat(response.getBody().getError().code()).isEqualTo("INTERNAL_ERROR");
        }

        @Test
        @DisplayName("should use custom code when provided")
        void shouldUseCustomCode() {
            KezaException ex = new KezaException("PAYMENT_FAILED", "M-Pesa timeout");

            ResponseEntity<ApiResponse<Void>> response = handler.handleKeza(ex);

            assertThat(response.getBody().getError().code()).isEqualTo("PAYMENT_FAILED");
            assertThat(response.getBody().getMessage()).isEqualTo("M-Pesa timeout");
        }
    }

    @Nested
    @DisplayName("General Exception")
    class GeneralExceptionTests {

        @Test
        @DisplayName("should return 500 with generic message for unexpected exceptions")
        void shouldReturn500WithGenericMessage() {
            Exception ex = new RuntimeException("NullPointerException somewhere");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(response.getBody().isSuccess()).isFalse();
            assertThat(response.getBody().getMessage()).isEqualTo("An unexpected error occurred");
            assertThat(response.getBody().getError().code()).isEqualTo("INTERNAL_ERROR");
            assertThat(response.getBody().getError().status()).isEqualTo(500);
        }

        @Test
        @DisplayName("should not leak internal exception details in response")
        void shouldNotLeakInternalDetails() {
            Exception ex = new RuntimeException("SQL syntax error at line 42");

            ResponseEntity<ApiResponse<Void>> response = handler.handleGeneral(ex);

            assertThat(response.getBody().getMessage()).doesNotContain("SQL");
            assertThat(response.getBody().getError().message()).doesNotContain("SQL");
        }
    }
}
