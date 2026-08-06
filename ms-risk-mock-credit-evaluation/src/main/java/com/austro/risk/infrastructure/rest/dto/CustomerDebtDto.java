package com.austro.risk.infrastructure.rest.dto;

import java.math.BigDecimal;

public record CustomerDebtDto(String debtName, BigDecimal monthlyPayment) {}
