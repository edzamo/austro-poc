package com.austro.orchestrator.application.service;

import com.austro.orchestrator.domain.model.EvaluationStatus;
import com.austro.orchestrator.domain.model.RiskData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests unitarios puros para la regla de negocio de aprobación crediticia.
 * No necesitan contexto de Quarkus — instancian el servicio directamente con new.
 *
 * Regla: APROBADO si score > 70 Y (deudas_mensuales + cuota) < (salario * 0.40)
 */
@DisplayName("CreditApprovalService - Regla de negocio de aprobación crediticia")
class CreditApprovalServiceTest {

    private CreditApprovalService service;

    // Escenario base: préstamo de $10,000 a 2 años con salario de $2,000
    // Cuota mensual = 10000 / (2*12) = 416.67
    // Capacidad máxima = 2000 * 0.40 = 800
    private static final BigDecimal REQUESTED_AMOUNT = new BigDecimal("10000");
    private static final int YEARS = 2;
    private static final BigDecimal SALARY = new BigDecimal("2000");

    @BeforeEach
    void setUp() {
        service = new CreditApprovalService();
    }

    @Nested
    @DisplayName("Casos de APROBADO")
    class ApprovedCases {

        @Test
        @DisplayName("Score > 70 y compromiso mensual por debajo del 40% del salario")
        void shouldApproveWhenScoreHighAndDebtLow() {
            // Cuota: 416.67, sin deudas previas. Total: 416.67 < 800 → APROBADO
            RiskData riskData = new RiskData(85, List.of());
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.APROBADO, result);
        }

        @Test
        @DisplayName("Score exactamente 71 (límite mínimo de aprobación)")
        void shouldApproveWhenScoreIsExactly71() {
            RiskData riskData = new RiskData(71, List.of());
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.APROBADO, result);
        }

        @Test
        @DisplayName("Con deuda previa pero sin superar el límite del 40%")
        void shouldApproveWithSmallExistingDebt() {
            // Deuda previa: $150. Cuota: $416.67. Total: $566.67 < $800 → APROBADO
            RiskData riskData = new RiskData(90, List.of(
                    new RiskData.Debt("Tarjeta", new BigDecimal("150.00"))
            ));
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.APROBADO, result);
        }

        @Test
        @DisplayName("Score máximo y sin deudas")
        void shouldApproveWithPerfectScore() {
            RiskData riskData = new RiskData(100, List.of());
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.APROBADO, result);
        }
    }

    @Nested
    @DisplayName("Casos de RECHAZADO")
    class RejectedCases {

        @Test
        @DisplayName("Score <= 70 debe ser rechazado sin importar las deudas")
        void shouldRejectWhenScoreIsExactly70() {
            RiskData riskData = new RiskData(70, List.of());
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.RECHAZADO, result);
        }

        @Test
        @DisplayName("Score = 0 debe ser rechazado")
        void shouldRejectWhenScoreIsZero() {
            RiskData riskData = new RiskData(0, List.of());
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.RECHAZADO, result);
        }

        @Test
        @DisplayName("Score alto pero deudas superan el 40% del salario")
        void shouldRejectWhenDebtExceedsLimit() {
            // Deuda previa: $500. Cuota: $416.67. Total: $916.67 > $800 → RECHAZADO
            RiskData riskData = new RiskData(95, List.of(
                    new RiskData.Debt("Hipoteca", new BigDecimal("500.00"))
            ));
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.RECHAZADO, result);
        }

        @Test
        @DisplayName("Score bajo Y deudas altas (doble rechazo)")
        void shouldRejectWhenBothConditionsFail() {
            RiskData riskData = new RiskData(30, List.of(
                    new RiskData.Debt("Hipoteca", new BigDecimal("600.00")),
                    new RiskData.Debt("Tarjeta", new BigDecimal("300.00"))
            ));
            EvaluationStatus result = service.evaluate(REQUESTED_AMOUNT, YEARS, SALARY, riskData);
            assertEquals(EvaluationStatus.RECHAZADO, result);
        }

        @Test
        @DisplayName("Cuota mensual por sí sola supera el 40% del salario")
        void shouldRejectWhenInstallmentAloneExceedsLimit() {
            // Monto: $30,000, 1 año, salario: $500
            // Cuota: 30000/12 = $2,500. Límite: $200 → RECHAZADO
            RiskData riskData = new RiskData(85, List.of());
            EvaluationStatus result = service.evaluate(
                    new BigDecimal("30000"), 1, new BigDecimal("500"), riskData);
            assertEquals(EvaluationStatus.RECHAZADO, result);
        }
    }
}
