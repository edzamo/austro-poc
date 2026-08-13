package com.austro.orchestrator.application.port.out;

import com.austro.orchestrator.domain.model.RiskData;

/**
 * Puerto de salida (driven port): define el contrato que la aplicación necesita
 * para obtener datos de riesgo de un servicio externo.
 * La implementación (adaptador) se encargará de la comunicación real (ej. REST,
 * gRPC).
 */
public interface RiskPort {

    /**
     * Obtiene los datos de riesgo (score y deudas) para una cédula específica.
     *
     * @param cedula la cédula del cliente a consultar.
     * @return un objeto RiskData que contiene el score y la lista de deudas.
     */
    RiskData getRiskData(String cedula);
}