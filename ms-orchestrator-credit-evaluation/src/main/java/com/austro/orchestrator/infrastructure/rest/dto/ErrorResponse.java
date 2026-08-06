package com.austro.orchestrator.infrastructure.rest.dto;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Record inmutable para respuestas de error.
 * El constructor compacto de 3 parámetros estampa el timestamp UTC automáticamente.
 */
public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {

    public ErrorResponse(int status, String error, String message) {
        this(status, error, message, LocalDateTime.now(ZoneOffset.UTC));
    }
}
