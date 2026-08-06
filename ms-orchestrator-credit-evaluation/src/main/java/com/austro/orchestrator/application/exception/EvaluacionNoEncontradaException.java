package com.austro.orchestrator.application.exception;

public class EvaluacionNoEncontradaException extends RuntimeException {

    public EvaluacionNoEncontradaException(Long id) {
        super("No se encontró una evaluación de crédito con id: " + id);
    }
}
