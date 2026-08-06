package com.austro.risk.infrastructure.rest;

import com.austro.risk.application.service.RiskService;
import com.austro.risk.domain.model.CustomerDebtsResult;
import com.austro.risk.domain.model.RiskScore;
import com.austro.risk.infrastructure.rest.dto.CustomerDebtDto;
import com.austro.risk.infrastructure.rest.dto.CustomerDebtsResponseDto;
import com.austro.risk.infrastructure.rest.dto.RiskScoreResponseDto;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/v1")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Risk Mock Service", description = "Endpoints para simulación de datos de riesgo crediticio")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class RiskResource {

    private final RiskService riskService;

    @GET
    @Path("/risk-score/{cedula}")
    @Operation(summary = "Obtiene el score de riesgo para una cédula",
               description = "Retorna un score aleatorio entre 0 y 100. Simula 2 segundos de latencia.")
    public Response getRiskScore(@PathParam("cedula") String cedula) {
        RiskScore riskScore = riskService.generateRiskScore(cedula);
        return Response.ok(new RiskScoreResponseDto(riskScore.cedula(), riskScore.score())).build();
    }

    @GET
    @Path("/customer-debts/{cedula}")
    @Operation(summary = "Obtiene las deudas del cliente",
               description = "Retorna entre 0 y 3 deudas aleatorias. Simula 1.5 segundos de latencia.")
    public Response getCustomerDebts(@PathParam("cedula") String cedula) {
        CustomerDebtsResult result = riskService.generateCustomerDebts(cedula);
        List<CustomerDebtDto> debtDtos = result.debts().stream()
                .map(debt -> new CustomerDebtDto(debt.debtName(), debt.monthlyPayment()))
                .toList();
        return Response.ok(new CustomerDebtsResponseDto(debtDtos)).build();
    }
}
