package com.austro.orchestrator.infrastructure.rest.dto;

import com.austro.orchestrator.domain.model.CreditEvaluation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Record inmutable para la respuesta de evaluación.
 * La factoría estática from() mapea desde la entidad de dominio.
 */
public record CreditEvaluationResponse(
        Long id,
        String cedula,
        BigDecimal requestedAmount,
        int years,
        BigDecimal salary,
        String finalStatus,
        LocalDateTime evaluationDate
) {
    public static CreditEvaluationResponse from(CreditEvaluation evaluation) {
        return new CreditEvaluationResponse(
                evaluation.id(),
                evaluation.cedula(),
                evaluation.requestedAmount(),
                evaluation.years(),
                evaluation.salary(),
                evaluation.finalStatus().name(),
                evaluation.evaluationDate()
        );
    }
}
