package com.example.supporthub.dto;

import java.time.Instant;
import java.util.List;

/**
 * Standard error body returned for every non-2xx response. {@code fieldErrors} is populated only
 * for validation (400) failures.
 */
public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> fieldErrors) {

    public static ErrorResponse of(int status, String error, String message, String path) {
        return new ErrorResponse(Instant.now(), status, error, message, path, null);
    }

    public static ErrorResponse of(int status, String error, String message, String path, List<String> fieldErrors) {
        return new ErrorResponse(Instant.now(), status, error, message, path, fieldErrors);
    }
}
