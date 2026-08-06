package com.austro.orchestrator.infrastructure.client.dto;

/**
 * Record para deserialización de la respuesta del endpoint /v1/risk-score/{cedula}.
 * Jackson 2.15+ (incluido en Quarkus 3.x) deserializa records de forma nativa
 * usando los nombres de los componentes como nombres de campo JSON.
 */
public record RiskScoreResponse(String cedula, int score) {}
