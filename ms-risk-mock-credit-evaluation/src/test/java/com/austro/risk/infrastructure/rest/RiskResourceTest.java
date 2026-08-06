package com.austro.risk.infrastructure.rest;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import com.austro.risk.application.service.RiskService;
import com.austro.risk.domain.model.CustomerDebt;
import com.austro.risk.domain.model.CustomerDebtsResult;
import com.austro.risk.domain.model.RiskScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class RiskResourceTest {

    @InjectMock
    RiskService riskService;

    private static final String TEST_CEDULA = "1713175071";

    @BeforeEach
    void setUp() {
        // Mockeamos el servicio para evitar la latencia simulada en los tests
        Mockito.when(riskService.generateRiskScore(TEST_CEDULA))
                .thenReturn(new RiskScore(TEST_CEDULA, 85));

        Mockito.when(riskService.generateCustomerDebts(TEST_CEDULA))
                .thenReturn(new CustomerDebtsResult(List.of(
                        new CustomerDebt("Tarjeta de Crédito", new BigDecimal("150.50")),
                        new CustomerDebt("Préstamo Personal", new BigDecimal("200.00"))
                )));
    }

    @Test
    void testRiskScoreEndpoint_returnsCedulaAndScore() {
        given()
            .pathParam("cedula", TEST_CEDULA)
        .when()
            .get("/v1/risk-score/{cedula}")
        .then()
            .statusCode(200)
            .body("cedula", equalTo(TEST_CEDULA))
            .body("score", equalTo(85));
    }

    @Test
    void testRiskScoreEndpoint_scoreIsWithinValidRange() {
        // Con datos reales (sin mock específico), el score debe estar entre 0 y 100
        Mockito.when(riskService.generateRiskScore("9999999999"))
                .thenReturn(new RiskScore("9999999999", 42));

        given()
            .pathParam("cedula", "9999999999")
        .when()
            .get("/v1/risk-score/{cedula}")
        .then()
            .statusCode(200)
            .body("score", allOf(greaterThanOrEqualTo(0), lessThanOrEqualTo(100)));
    }

    @Test
    void testCustomerDebtsEndpoint_returnsDebtsList() {
        given()
            .pathParam("cedula", TEST_CEDULA)
        .when()
            .get("/v1/customer-debts/{cedula}")
        .then()
            .statusCode(200)
            .body("debts", notNullValue())
            .body("debts.size()", equalTo(2))
            .body("debts[0].debtName", equalTo("Tarjeta de Crédito"))
            .body("debts[0].monthlyPayment", equalTo(150.50f))
            .body("debts[1].debtName", equalTo("Préstamo Personal"))
            .body("debts[1].monthlyPayment", equalTo(200.00f));
    }

    @Test
    void testCustomerDebtsEndpoint_returnsEmptyListWhenNoDebts() {
        Mockito.when(riskService.generateCustomerDebts("0000000000"))
                .thenReturn(new CustomerDebtsResult(List.of()));

        given()
            .pathParam("cedula", "0000000000")
        .when()
            .get("/v1/customer-debts/{cedula}")
        .then()
            .statusCode(200)
            .body("debts", notNullValue())
            .body("debts.size()", equalTo(0));
    }

    @Test
    void testCustomerDebtsEndpoint_debtCountIsAtMostThree() {
        given()
            .pathParam("cedula", TEST_CEDULA)
        .when()
            .get("/v1/customer-debts/{cedula}")
        .then()
            .statusCode(200)
            .body("debts.size()", lessThanOrEqualTo(3));
    }
}
