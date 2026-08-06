package com.austro.orchestrator.infrastructure.rest;

import com.austro.orchestrator.application.port.in.EvaluateCreditUseCase;
import com.austro.orchestrator.application.port.in.GetEvaluationsUseCase;
import com.austro.orchestrator.domain.model.CreditEvaluation;
import com.austro.orchestrator.infrastructure.rest.dto.CreditEvaluationRequest;
import com.austro.orchestrator.infrastructure.rest.dto.CreditEvaluationResponse;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/v1/credit-evaluations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Credit Evaluations", description = "Endpoints para evaluación y consulta de créditos")
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class CreditEvaluationResource {

    private final EvaluateCreditUseCase evaluateCreditUseCase;
    private final GetEvaluationsUseCase getEvaluationsUseCase;

    @POST
    @Operation(summary = "Evalúa una solicitud de crédito",
               description = "Valida la cédula, consulta el servicio de riesgo, aplica las reglas de negocio y persiste el resultado.")
    public Response evaluate(@Valid CreditEvaluationRequest request) {
        CreditEvaluation evaluation = evaluateCreditUseCase.evaluate(
                request.cedula(),
                request.requestedAmount(),
                request.years(),
                request.salary()
        );
        return Response.status(Response.Status.CREATED)
                .entity(CreditEvaluationResponse.from(evaluation))
                .build();
    }

    @GET
    @Operation(summary = "Obtiene el historial de evaluaciones",
               description = "Retorna todas las evaluaciones de crédito registradas en el sistema.")
    public Response findAll() {
        List<CreditEvaluationResponse> responses = getEvaluationsUseCase.findAll()
                .stream()
                .map(CreditEvaluationResponse::from)
                .toList();
        return Response.ok(responses).build();
    }
}
