package com.austro.orchestrator.application.service;

import com.austro.orchestrator.domain.model.EvaluationStatus;
import com.austro.orchestrator.domain.model.RiskData;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Servicio de aplicación que aplica la regla de aprobación crediticia.
 *
 * Regla: APROBADO si:
 *   - score > 70
 *   - (Σ deudas_mensuales + (monto / (años * 12))) < (salario * 0.40)
 */
@ApplicationScoped
public class CreditApprovalService {

    private static final int MINIMUM_SCORE = 70;
    private static final BigDecimal DEBT_TO_INCOME_RATIO = new BigDecimal("0.40");

    public EvaluationStatus evaluate(
            BigDecimal requestedAmount,
            int years,
            BigDecimal salary,
            RiskData riskData) {

        if (riskData.score() <= MINIMUM_SCORE) {
            return EvaluationStatus.RECHAZADO;
        }

        BigDecimal totalMonthlyDebt = riskData.debts().stream()
                .map(RiskData.Debt::monthlyPayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal monthlyInstallment = requestedAmount
                .divide(BigDecimal.valueOf((long) years * 12), 2, RoundingMode.HALF_UP);

        BigDecimal totalMonthlyCommitment = totalMonthlyDebt.add(monthlyInstallment);
        BigDecimal maxAllowedPayment = salary.multiply(DEBT_TO_INCOME_RATIO);

        if (totalMonthlyCommitment.compareTo(maxAllowedPayment) < 0) {
            return EvaluationStatus.APROBADO;
        }

        return EvaluationStatus.RECHAZADO;
    }
}
