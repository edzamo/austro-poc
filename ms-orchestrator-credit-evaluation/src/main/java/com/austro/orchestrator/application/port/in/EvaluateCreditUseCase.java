package com.austro.orchestrator.application.port.in;

import com.austro.orchestrator.domain.model.CreditEvaluation;

import java.math.BigDecimal;

/**
 * Puerto de entrada (driving port): define el contrato que la capa de aplicación
 * expone hacia los adaptadores de entrada (REST, mensajería, CLI, etc.).
 * Vive en application porque delimita la frontera de la aplicación, no del dominio puro.
 */
public interface EvaluateCreditUseCase {

    CreditEvaluation evaluate(String cedula, BigDecimal requestedAmount, int years, BigDecimal salary);
}
