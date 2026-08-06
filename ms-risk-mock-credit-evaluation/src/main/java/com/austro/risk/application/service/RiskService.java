package com.austro.risk.application.service;

import com.austro.risk.domain.model.CustomerDebt;
import com.austro.risk.domain.model.CustomerDebtsResult;
import com.austro.risk.domain.model.RiskScore;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ApplicationScoped
public class RiskService {

    private static final Random RANDOM = new Random();

    private static final List<String> DEBT_NAMES = List.of(
            "Tarjeta de Crédito",
            "Préstamo Hipotecario",
            "Préstamo Personal",
            "Crédito Automotriz",
            "Línea de Crédito"
    );

    /**
     * Genera un score de riesgo aleatorio entre 0 y 100.
     * Simula una latencia interna de 2 segundos para representar
     * la consulta a un bureau de crédito externo.
     */
    public RiskScore generateRiskScore(String cedula) {
        simulateLatency(2000);
        int score = RANDOM.nextInt(101);
        return new RiskScore(cedula, score);
    }

    /**
     * Genera entre 0 y 3 deudas aleatorias para el cliente.
     * Simula una latencia interna de 1.5 segundos.
     */
    public CustomerDebtsResult generateCustomerDebts(String cedula) {
        simulateLatency(1500);
        int numberOfDebts = RANDOM.nextInt(4);
        List<CustomerDebt> debts = new ArrayList<>();

        List<String> shuffledNames = new ArrayList<>(DEBT_NAMES);
        java.util.Collections.shuffle(shuffledNames, RANDOM);

        for (int i = 0; i < numberOfDebts; i++) {
            BigDecimal monthlyPayment = BigDecimal.valueOf(50 + RANDOM.nextInt(951))
                    .setScale(2, RoundingMode.HALF_UP);
            debts.add(new CustomerDebt(shuffledNames.get(i), monthlyPayment));
        }

        return new CustomerDebtsResult(debts);
    }

    private void simulateLatency(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
