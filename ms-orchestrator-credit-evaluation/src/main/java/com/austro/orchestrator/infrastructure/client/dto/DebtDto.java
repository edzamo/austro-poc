package com.austro.orchestrator.infrastructure.client.dto;

import java.math.BigDecimal;

/**
 * Record para cada deuda dentro de la respuesta del endpoint /v1/customer-debts/{cedula}.
 */
public record DebtDto(String debtName, BigDecimal monthlyPayment) {}
