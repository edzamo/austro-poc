package com.austro.risk.domain.model;

import java.math.BigDecimal;

public record CustomerDebt(String debtName, BigDecimal monthlyPayment) {}
