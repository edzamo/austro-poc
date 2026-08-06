package com.austro.orchestrator.infrastructure.client;

import com.austro.orchestrator.application.exception.RiskServiceUnavailableException;
import com.austro.orchestrator.application.port.out.RiskServicePort;
import com.austro.orchestrator.domain.model.RiskData;
import com.austro.orchestrator.infrastructure.client.dto.CustomerDebtsResponse;
import com.austro.orchestrator.infrastructure.client.dto.RiskScoreResponse;
import jakarta.enterprise.context.ApplicationScoped;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import java.util.List;

/**
 * Adaptador de salida: implementa RiskServicePort mediante el cliente REST declarativo.
 * Usa field injection para @RestClient porque los qualificadores CDI no se pueden
 * expresar en constructores generados por Lombok.
 */
@Slf4j
@ApplicationScoped
public class RiskServiceAdapter implements RiskServicePort {

    @RestClient
    RiskServiceClient riskServiceClient;

    @Override
    public RiskData getRiskData(String cedula) {
        try {
            log.info("Obteniendo score de riesgo para cédula: {}", cedula);
            RiskScoreResponse scoreResponse = riskServiceClient.getRiskScore(cedula);

            log.info("Obteniendo deudas del cliente para cédula: {}", cedula);
            CustomerDebtsResponse debtsResponse = riskServiceClient.getCustomerDebts(cedula);

            List<RiskData.Debt> debts = debtsResponse.debts() == null ? List.of() :
                    debtsResponse.debts().stream()
                            .map(d -> new RiskData.Debt(d.debtName(), d.monthlyPayment()))
                            .toList();

            return new RiskData(scoreResponse.score(), debts);

        } catch (Exception ex) {
            log.error("Error al consultar el servicio de riesgo para cédula: {}", cedula, ex);
            throw new RiskServiceUnavailableException(
                    "El servicio de evaluación de riesgo no está disponible. Intente más tarde.", ex);
        }
    }
}
