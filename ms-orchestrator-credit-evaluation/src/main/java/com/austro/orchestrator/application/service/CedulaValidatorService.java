package com.austro.orchestrator.application.service;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Servicio de aplicación: valida cédulas ecuatorianas mediante el algoritmo Módulo 10.
 *
 * Algoritmo:
 * 1. Exactamente 10 dígitos numéricos.
 * 2. Los dos primeros dígitos representan la provincia (01-24 o 30).
 * 3. El tercer dígito debe ser < 6 (persona natural).
 * 4. Módulo 10 sobre los primeros 9 dígitos verifica el dígito de control.
 */
@ApplicationScoped
public class CedulaValidatorService {

    private static final int CEDULA_LENGTH = 10;
    private static final int MAX_PROVINCE_CODE = 24;

    public boolean isValid(String cedula) {
        if (cedula == null || cedula.length() != CEDULA_LENGTH) {
            return false;
        }

        if (!cedula.matches("\\d+")) {
            return false;
        }

        int provinceCode = Integer.parseInt(cedula.substring(0, 2));
        if (provinceCode < 1 || (provinceCode > MAX_PROVINCE_CODE && provinceCode != 30)) {
            return false;
        }

        int thirdDigit = Character.getNumericValue(cedula.charAt(2));
        if (thirdDigit >= 6) {
            return false;
        }

        return validateModuloTen(cedula);
    }

    private boolean validateModuloTen(String cedula) {
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            int digit = Character.getNumericValue(cedula.charAt(i));
            // Posiciones impares (índice par: 0,2,4,6,8) se multiplican por 2
            if (i % 2 == 0) {
                digit *= 2;
                if (digit > 9) {
                    digit -= 9;
                }
            }
            sum += digit;
        }

        int checkDigit = Character.getNumericValue(cedula.charAt(9));
        int expectedCheckDigit = (10 - (sum % 10)) % 10;

        return checkDigit == expectedCheckDigit;
    }
}
