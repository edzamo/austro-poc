package com.austro.risk.infrastructure.rest.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public record ErrorResponseDto(int status, String error, String message, LocalDateTime timestamp) {

    public ErrorResponseDto(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now(ZoneOffset.UTC));
    }
}
