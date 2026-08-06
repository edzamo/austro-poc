package com.austro.risk.infrastructure.rest.exception;

import com.austro.risk.application.exception.RiskServiceException;
import com.austro.risk.infrastructure.rest.dto.ErrorResponseDto;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

    @Override
    public Response toResponse(Exception exception) {

        if (exception instanceof RiskServiceException ex) {
            log.error("Error en generación de datos de riesgo: {}", ex.getMessage());
            return build(500, "Error en Servicio de Riesgo", ex.getMessage());
        }

        if (exception instanceof WebApplicationException ex) {
            return ex.getResponse();
        }

        log.error("Error no controlado en el servicio de riesgo", exception);
        return build(500, "Error Interno", "Error interno del servicio de riesgo. Por favor intente más tarde.");
    }

    private Response build(int status, String error, String message) {
        return Response.status(status)
                .entity(new ErrorResponseDto(status, error, message))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
