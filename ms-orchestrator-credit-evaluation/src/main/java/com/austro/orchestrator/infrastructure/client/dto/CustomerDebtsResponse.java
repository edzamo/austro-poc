package com.austro.orchestrator.infrastructure.client.dto;

import java.util.List;

/**
 * Record para deserialización de la respuesta del endpoint /v1/customer-debts/{cedula}.
 */
public record CustomerDebtsResponse(List<DebtDto> debts) {}
