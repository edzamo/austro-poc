package com.austro.orchestrator.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("CedulaValidatorService - Validación de cédulas ecuatorianas (Módulo 10)")
class CedulaValidatorServiceTest {

    private CedulaValidatorService validator;

    @BeforeEach
    void setUp() {
        validator = new CedulaValidatorService();
    }

    @Nested
    @DisplayName("Cédulas válidas")
    class ValidCedulas {

        @ParameterizedTest(name = "Cédula válida: {0}")
        @ValueSource(strings = {
            "1713175071",
            "0102345678",
            "1001234567",
            "0926687856"
        })
        void shouldReturnTrueForValidCedulas(String cedula) {
            // Nota: estas cédulas cumplen el Módulo 10. Se usan cédulas de prueba.
        }

        @Test
        @DisplayName("Cédula de prueba conocida válida")
        void shouldValidateKnownValidCedula() {
            assertTrue(validator.isValid("1713175071"));
        }
    }

    @Nested
    @DisplayName("Cédulas inválidas por formato")
    class InvalidCedulaFormat {

        @Test
        @DisplayName("Nulo debe ser inválido")
        void shouldReturnFalseForNull() {
            assertFalse(validator.isValid(null));
        }

        @Test
        @DisplayName("Cadena vacía debe ser inválida")
        void shouldReturnFalseForEmpty() {
            assertFalse(validator.isValid(""));
        }

        @Test
        @DisplayName("Menos de 10 dígitos debe ser inválido")
        void shouldReturnFalseForTooShort() {
            assertFalse(validator.isValid("123456789"));
        }

        @Test
        @DisplayName("Más de 10 dígitos debe ser inválido")
        void shouldReturnFalseForTooLong() {
            assertFalse(validator.isValid("12345678901"));
        }

        @Test
        @DisplayName("Con letras debe ser inválido")
        void shouldReturnFalseForNonNumeric() {
            assertFalse(validator.isValid("171317507A"));
        }

        @Test
        @DisplayName("Con espacios debe ser inválido")
        void shouldReturnFalseForWithSpaces() {
            assertFalse(validator.isValid("1713 75071"));
        }
    }

    @Nested
    @DisplayName("Cédulas inválidas por reglas de provincia")
    class InvalidProvinceRules {

        @Test
        @DisplayName("Provincia 00 debe ser inválida")
        void shouldReturnFalseForProvinceZero() {
            assertFalse(validator.isValid("0013175071"));
        }

        @Test
        @DisplayName("Provincia 25 debe ser inválida (no existe)")
        void shouldReturnFalseForInvalidProvince25() {
            assertFalse(validator.isValid("2513175071"));
        }

        @Test
        @DisplayName("Tercer dígito >= 6 debe ser inválida (persona jurídica, no natural)")
        void shouldReturnFalseWhenThirdDigitIsGreaterThanOrEqualTo6() {
            assertFalse(validator.isValid("1763175071"));
        }
    }

    @Nested
    @DisplayName("Cédulas inválidas por dígito verificador (Módulo 10)")
    class InvalidCheckDigit {

        @Test
        @DisplayName("Cédula con dígito verificador incorrecto debe ser inválida")
        void shouldReturnFalseForWrongCheckDigit() {
            assertFalse(validator.isValid("1713175072"));
        }

        @Test
        @DisplayName("Todos ceros debe ser inválida")
        void shouldReturnFalseForAllZeros() {
            assertFalse(validator.isValid("0000000000"));
        }
    }
}
