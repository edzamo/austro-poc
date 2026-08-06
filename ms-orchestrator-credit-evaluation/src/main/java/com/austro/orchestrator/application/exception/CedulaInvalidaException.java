package com.austro.orchestrator.application.exception;

public class CedulaInvalidaException extends RuntimeException {

    public CedulaInvalidaException(String cedula) {
        super("La cédula '" + cedula + "' no es válida según el algoritmo Módulo 10 de Ecuador.");
    }
}
