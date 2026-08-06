package com.austro.orchestrator.domain.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Value Object como Java 21 Record: inmutabilidad garantizada por el compilador.
 * El constructor compacto aplica la copia defensiva de la lista antes de almacenarla.
 */
public record RiskData(int score, List<Debt> debts) {

    public RiskData {
        debts = List.copyOf(debts);
    }

    public record Debt(String debtName, BigDecimal monthlyPayment) {}
}
