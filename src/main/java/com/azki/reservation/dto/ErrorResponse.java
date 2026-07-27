package com.azki.reservation.dto;

import java.time.LocalDateTime;
import java.util.List;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        List<FieldError> errors) {

    public record FieldError(String field, String message) {
    }
}