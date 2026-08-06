package com.austro.orchestrator.domain.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Entidad de dominio como Java 21 Record: inmutable por construcción.
 * Los records generan automáticamente constructor canónico, accessors,
 * equals/hashCode/toString — sin código boilerplate.
 *
 * Las factorías estáticas preservan la intención semántica:
 *  - create()       → nueva evaluación (id aún no asignado)
 *  - reconstitute() → reconstruir desde la base de datos (id ya existe)
 */
public record CreditEvaluation(
        Long id,
        String cedula,
        BigDecimal requestedAmount,
        int years,
        BigDecimal salary,
        EvaluationStatus finalStatus,
        LocalDateTime evaluationDate
) {

    public static CreditEvaluation create(
            String cedula,
            BigDecimal requestedAmount,
            int years,
            BigDecimal salary,
            EvaluationStatus finalStatus) {
        return new CreditEvaluation(null, cedula, requestedAmount, years, salary, finalStatus, LocalDateTime.now(ZoneOffset.UTC));
    }

    public static CreditEvaluation reconstitute(
            Long id,
            String cedula,
            BigDecimal requestedAmount,
            int years,
            BigDecimal salary,
            EvaluationStatus finalStatus,
            LocalDateTime evaluationDate) {
        return new CreditEvaluation(id, cedula, requestedAmount, years, salary, finalStatus, evaluationDate);
    }
}
