package com.austro.orchestrator.infrastructure.client;

import com.austro.orchestrator.infrastructure.client.dto.CustomerDebtsResponse;
import com.austro.orchestrator.infrastructure.client.dto.RiskScoreResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

/**
 * Cliente REST declarativo (MicroProfile REST Client) para comunicarse con
 * ms-risk-mock-credit-evaluation. La interfaz actúa como contrato tipado.
 *
 * Elección de REST sobre gRPC: ver README.md para la justificación completa.
 */
@RegisterRestClient(configKey = "risk-service")
@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
public interface RiskServiceClient {

    @GET
    @Path("/risk-score/{cedula}")
    RiskScoreResponse getRiskScore(@PathParam("cedula") String cedula);

    @GET
    @Path("/customer-debts/{cedula}")
    CustomerDebtsResponse getCustomerDebts(@PathParam("cedula") String cedula);
}
