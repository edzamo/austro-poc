package com.austro.orchestrator.application.port.in;

import com.austro.orchestrator.domain.model.CreditEvaluation;

import java.util.List;

/**
 * Puerto de entrada (driving port): contrato para recuperar el historial de evaluaciones.
 * Reside en application porque es parte de la frontera que la aplicación expone al exterior.
 */
public interface GetEvaluationsUseCase {

    List<CreditEvaluation> findAll();
}
