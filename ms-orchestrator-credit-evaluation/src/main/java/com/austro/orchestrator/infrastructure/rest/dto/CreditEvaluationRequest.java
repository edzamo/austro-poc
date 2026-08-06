package com.austro.orchestrator.infrastructure.rest.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Record inmutable para la solicitud de evaluación.
 * Jakarta Validation funciona sobre los componentes del record en Quarkus 3.x.
 */
public record CreditEvaluationRequest(
        @NotBlank(message = "La cédula es obligatoria")
        String cedula,

        @NotNull(message = "El monto solicitado es obligatorio")
        @Positive(message = "El monto solicitado debe ser positivo")
        BigDecimal requestedAmount,

        @NotNull(message = "El número de años es obligatorio")
        @Positive(message = "El número de años debe ser positivo")
        Integer years,

        @NotNull(message = "El salario es obligatorio")
        @Positive(message = "El salario debe ser positivo")
        BigDecimal salary
) {}
