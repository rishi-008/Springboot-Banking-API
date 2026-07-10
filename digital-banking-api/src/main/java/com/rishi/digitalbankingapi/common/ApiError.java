package com.rishi.digitalbankingapi.common;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.List;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldViolation> fieldErrors
) {

    public static ApiError of(HttpStatus status, String code, String message, String path) {
        return new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), code, message, path, null);
    }

    public static ApiError ofValidation(String path, List<FieldViolation> fieldErrors) {
        return new ApiError(Instant.now(), HttpStatus.BAD_REQUEST.value(), HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_FAILED", "Validation failed", path, fieldErrors);
    }

    public record FieldViolation(String field, String message) {
    }
}
