package com.austro.orchestrator.infrastructure.rest;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkiverse.wiremock.devservice.ConnectWireMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.equalTo;

/**
 * Tests de integración para el endpoint de evaluación de créditos.
 * Usa WireMock para simular el ms-risk-mock-credit-evaluation de forma
 * determinista y sin latencia, garantizando tests rápidos y repetibles.
 */
@QuarkusTest
@ConnectWireMock
@DisplayName("CreditEvaluationResource - Tests de Integración")
class CreditEvaluationResourceTest {

    WireMock wireMock;

    private static final String VALID_CEDULA = "1713175071";
    private static final String EVALUATION_PATH = "/v1/credit-evaluations";

    @BeforeEach
    void setUp() {
        wireMock.resetToDefaultMappings();
    }

    private void stubHighScore() {
        wireMock.register(get(urlPathMatching("/v1/risk-score/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"cedula": "%s", "score": 85}
                            """.formatted(VALID_CEDULA))));

        wireMock.register(get(urlPathMatching("/v1/customer-debts/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"debts": []}
                            """)));
    }

    private void stubLowScore() {
        wireMock.register(get(urlPathMatching("/v1/risk-score/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"cedula": "%s", "score": 40}
                            """.formatted(VALID_CEDULA))));

        wireMock.register(get(urlPathMatching("/v1/customer-debts/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"debts": []}
                            """)));
    }

    private void stubHighScoreWithHighDebt() {
        wireMock.register(get(urlPathMatching("/v1/risk-score/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"cedula": "%s", "score": 90}
                            """.formatted(VALID_CEDULA))));

        wireMock.register(get(urlPathMatching("/v1/customer-debts/.*"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                            {"debts": [
                                {"debtName": "Hipoteca", "monthlyPayment": 800.00},
                                {"debtName": "Tarjeta", "monthlyPayment": 500.00}
                            ]}
                            """)));
    }

    @Nested
    @DisplayName("POST /v1/credit-evaluations - Evaluación exitosa")
    class EvaluationEndpoint {

        @Test
        @DisplayName("Debe retornar APROBADO cuando score > 70 y deudas son manejables")
        void shouldReturnApprovedWhenScoreHighAndDebtLow() {
            stubHighScore();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "%s",
                        "requestedAmount": 10000,
                        "years": 5,
                        "salary": 2000
                    }
                    """.formatted(VALID_CEDULA))
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(201)
                .body("finalStatus", equalTo("APROBADO"))
                .body("cedula", equalTo(VALID_CEDULA))
                .body("id", notNullValue())
                .body("evaluationDate", notNullValue());
        }

        @Test
        @DisplayName("Debe retornar RECHAZADO cuando el score es bajo")
        void shouldReturnRejectedWhenScoreLow() {
            stubLowScore();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "%s",
                        "requestedAmount": 10000,
                        "years": 5,
                        "salary": 2000
                    }
                    """.formatted(VALID_CEDULA))
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(201)
                .body("finalStatus", equalTo("RECHAZADO"));
        }

        @Test
        @DisplayName("Debe retornar RECHAZADO cuando las deudas superan el 40% del salario")
        void shouldReturnRejectedWhenDebtTooHigh() {
            stubHighScoreWithHighDebt();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "%s",
                        "requestedAmount": 15000,
                        "years": 3,
                        "salary": 1500
                    }
                    """.formatted(VALID_CEDULA))
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(201)
                .body("finalStatus", equalTo("RECHAZADO"));
        }

        @Test
        @DisplayName("La evaluación debe persistirse y aparecer en el historial")
        void shouldPersistEvaluationAndAppearInHistory() {
            stubHighScore();

            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "%s",
                        "requestedAmount": 5000,
                        "years": 2,
                        "salary": 3000
                    }
                    """.formatted(VALID_CEDULA))
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(201);

            given()
            .when()
                .get(EVALUATION_PATH)
            .then()
                .statusCode(200)
                .body("size()", greaterThan(0));
        }
    }

    @Nested
    @DisplayName("POST /v1/credit-evaluations - Validaciones de entrada")
    class ValidationCases {

        @Test
        @DisplayName("Debe retornar 400 cuando la cédula es inválida (Módulo 10)")
        void shouldReturn400ForInvalidCedula() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "1234567890",
                        "requestedAmount": 10000,
                        "years": 5,
                        "salary": 2000
                    }
                    """)
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(400)
                .body("message", containsString("cédula"));
        }

        @Test
        @DisplayName("Debe retornar 400 cuando el monto es negativo")
        void shouldReturn400ForNegativeAmount() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "%s",
                        "requestedAmount": -5000,
                        "years": 5,
                        "salary": 2000
                    }
                    """.formatted(VALID_CEDULA))
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Debe retornar 400 cuando faltan campos obligatorios")
        void shouldReturn400WhenMissingRequiredFields() {
            given()
                .contentType(ContentType.JSON)
                .body("{}")
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(400);
        }

        @Test
        @DisplayName("Debe retornar 400 cuando la cédula está vacía")
        void shouldReturn400ForEmptyCedula() {
            given()
                .contentType(ContentType.JSON)
                .body("""
                    {
                        "cedula": "",
                        "requestedAmount": 10000,
                        "years": 5,
                        "salary": 2000
                    }
                    """)
            .when()
                .post(EVALUATION_PATH)
            .then()
                .statusCode(400);
        }
    }

    @Nested
    @DisplayName("GET /v1/credit-evaluations - Historial")
    class HistoryEndpoint {

        @Test
        @DisplayName("Debe retornar una lista (puede estar vacía al inicio)")
        void shouldReturnListOfEvaluations() {
            given()
            .when()
                .get(EVALUATION_PATH)
            .then()
                .statusCode(200)
                .body("$", instanceOf(java.util.List.class));
        }
    }
}
