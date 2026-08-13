package com.austro.orchestrator.application.port.out;

import com.austro.orchestrator.domain.model.CreditEvaluation;

import java.util.List;

/**
 * Puerto de salida (driven port): define el contrato que la aplicación necesita
 * para interactuar con la capa de persistencia.
 * La implementación (adaptador) estará en la capa de infraestructura.
 */
public interface CreditEvaluationPort {

    /**
     * Guarda una evaluación de crédito en el repositorio.
     *
     * @param evaluation el objeto de dominio a persistir.
     * @return la evaluación de crédito persistida, posiblemente con el ID asignado.
     */
    CreditEvaluation save(CreditEvaluation evaluation);

    /**
     * Obtiene una lista de todas las evaluaciones de crédito almacenadas.
     *
     * @return una lista de objetos de dominio CreditEvaluation.
     */
    List<CreditEvaluation> listAllEvaluations();
}