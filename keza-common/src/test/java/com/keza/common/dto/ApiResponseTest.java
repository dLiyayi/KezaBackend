package com.keza.common.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ApiResponse")
class ApiResponseTest {

    @Nested
    @DisplayName("success(data)")
    class SuccessWithData {

        @Test
        @DisplayName("should create successful response with data")
        void shouldCreateSuccessWithData() {
            ApiResponse<String> response = ApiResponse.success("test-data");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("test-data");
            assertThat(response.getMessage()).isNull();
            assertThat(response.getError()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should accept null data")
        void shouldAcceptNullData() {
            ApiResponse<String> response = ApiResponse.success(null);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isNull();
        }

        @Test
        @DisplayName("should accept complex object as data")
        void shouldAcceptComplexData() {
            List<Integer> data = List.of(1, 2, 3);
            ApiResponse<List<Integer>> response = ApiResponse.success(data);

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).containsExactly(1, 2, 3);
        }
    }

    @Nested
    @DisplayName("success(data, message)")
    class SuccessWithDataAndMessage {

        @Test
        @DisplayName("should create successful response with data and message")
        void shouldCreateSuccessWithDataAndMessage() {
            ApiResponse<String> response = ApiResponse.success("data", "Operation completed");

            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getData()).isEqualTo("data");
            assertThat(response.getMessage()).isEqualTo("Operation completed");
            assertThat(response.getError()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("error(message, apiError)")
    class ErrorWithApiError {

        @Test
        @DisplayName("should create error response with message and ApiError")
        void shouldCreateErrorWithApiError() {
            ApiError error = ApiError.of(404, "NOT_FOUND", "Resource not found");
            ApiResponse<Void> response = ApiResponse.error("Not found", error);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Not found");
            assertThat(response.getError()).isNotNull();
            assertThat(response.getError().status()).isEqualTo(404);
            assertThat(response.getError().code()).isEqualTo("NOT_FOUND");
            assertThat(response.getError().message()).isEqualTo("Resource not found");
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("should include field errors when present")
        void shouldIncludeFieldErrors() {
            List<ApiError.FieldError> fieldErrors = List.of(
                    new ApiError.FieldError("email", "must not be blank"),
                    new ApiError.FieldError("name", "size must be between 2 and 100")
            );
            ApiError error = ApiError.withFieldErrors(400, "VALIDATION_ERROR", "Validation failed", fieldErrors);
            ApiResponse<Void> response = ApiResponse.error("Validation failed", error);

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getError().fieldErrors()).hasSize(2);
            assertThat(response.getError().fieldErrors().get(0).field()).isEqualTo("email");
            assertThat(response.getError().fieldErrors().get(1).field()).isEqualTo("name");
        }
    }

    @Nested
    @DisplayName("error(message)")
    class ErrorWithMessageOnly {

        @Test
        @DisplayName("should create error response with message only")
        void shouldCreateErrorWithMessage() {
            ApiResponse<Void> response = ApiResponse.error("Something went wrong");

            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("Something went wrong");
            assertThat(response.getError()).isNull();
            assertThat(response.getData()).isNull();
            assertThat(response.getTimestamp()).isNotNull();
        }
    }

    @Nested
    @DisplayName("ApiError factory methods")
    class ApiErrorFactoryMethods {

        @Test
        @DisplayName("ApiError.of should create error without field errors")
        void shouldCreateApiErrorWithoutFieldErrors() {
            ApiError error = ApiError.of(500, "INTERNAL_ERROR", "Server error");

            assertThat(error.status()).isEqualTo(500);
            assertThat(error.code()).isEqualTo("INTERNAL_ERROR");
            assertThat(error.message()).isEqualTo("Server error");
            assertThat(error.fieldErrors()).isNull();
        }

        @Test
        @DisplayName("ApiError.withFieldErrors should create error with field errors")
        void shouldCreateApiErrorWithFieldErrors() {
            List<ApiError.FieldError> fieldErrors = List.of(
                    new ApiError.FieldError("phone", "invalid format")
            );
            ApiError error = ApiError.withFieldErrors(400, "VALIDATION_ERROR", "Invalid input", fieldErrors);

            assertThat(error.status()).isEqualTo(400);
            assertThat(error.code()).isEqualTo("VALIDATION_ERROR");
            assertThat(error.message()).isEqualTo("Invalid input");
            assertThat(error.fieldErrors()).hasSize(1);
            assertThat(error.fieldErrors().get(0).field()).isEqualTo("phone");
            assertThat(error.fieldErrors().get(0).message()).isEqualTo("invalid format");
        }
    }

    @Nested
    @DisplayName("timestamp")
    class Timestamp {

        @Test
        @DisplayName("should automatically set timestamp on all factory methods")
        void shouldSetTimestamp() {
            assertThat(ApiResponse.success("data").getTimestamp()).isNotNull();
            assertThat(ApiResponse.success("data", "msg").getTimestamp()).isNotNull();
            assertThat(ApiResponse.error("err").getTimestamp()).isNotNull();
            assertThat(ApiResponse.error("err", ApiError.of(500, "X", "Y")).getTimestamp()).isNotNull();
        }
    }
}
