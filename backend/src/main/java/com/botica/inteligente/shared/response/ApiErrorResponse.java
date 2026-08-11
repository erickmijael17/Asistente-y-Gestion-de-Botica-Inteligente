package com.botica.inteligente.shared.response;

import java.time.LocalDateTime;
import java.util.List;

public record ApiErrorResponse(
        boolean success,
        String message,
        List<FieldValidationError> errors,
        String path,
        LocalDateTime timestamp
) {
    public static ApiErrorResponse of(String message, List<FieldValidationError> errors, String path) {
        return new ApiErrorResponse(false, message, errors, path, LocalDateTime.now());
    }
}
