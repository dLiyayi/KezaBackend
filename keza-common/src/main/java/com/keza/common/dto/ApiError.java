package com.keza.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {
    public record FieldError(String field, String message) {}

    public static ApiError of(int status, String code, String message) {
        return new ApiError(status, code, message, null);
    }

    public static ApiError withFieldErrors(int status, String code, String message, List<FieldError> fieldErrors) {
        return new ApiError(status, code, message, fieldErrors);
    }
}
