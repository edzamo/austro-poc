package com.austro.orchestrator.infrastructure.rest.exception;

import com.austro.orchestrator.application.exception.CedulaInvalidaException;
import com.austro.orchestrator.application.exception.EvaluacionNoEncontradaException;
import com.austro.orchestrator.application.exception.RiskServiceUnavailableException;
import com.austro.orchestrator.infrastructure.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof CedulaInvalidaException ex) {
            log.warn("Cédula inválida rechazada: {}", ex.getMessage());
            return build(422, "Cédula Inválida", ex.getMessage());
        }

        if (exception instanceof EvaluacionNoEncontradaException ex) {
            log.warn("Recurso no encontrado: {}", ex.getMessage());
            return build(404, "No Encontrado", ex.getMessage());
        }

        if (exception instanceof RiskServiceUnavailableException ex) {
            log.error("Servicio de riesgo no disponible: {}", ex.getMessage());
            return build(503, "Servicio No Disponible", ex.getMessage());
        }

        if (exception instanceof ConstraintViolationException ex) {
            String detail = ex.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                    .collect(Collectors.joining("; "));
            log.warn("Validación fallida: {}", detail);
            return build(422, "Error de Validación", detail);
        }

        if (exception instanceof WebApplicationException ex) {
            return ex.getResponse();
        }

        log.error("Error no controlado en el sistema", exception);
        return build(500, "Error Interno", "Error interno del sistema. Por favor intente más tarde.");
    }

    private Response build(int status, String error, String message) {
        return Response.status(status)
                .entity(new ErrorResponse(status, error, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
