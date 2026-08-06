package com.austro.orchestrator.application.usecase;

import com.austro.orchestrator.application.port.in.EvaluateCreditUseCase;
import com.austro.orchestrator.application.port.out.CreditEvaluationRepository;
import com.austro.orchestrator.application.port.out.RiskServicePort;
import com.austro.orchestrator.application.service.CedulaValidatorService;
import com.austro.orchestrator.application.service.CreditApprovalService;
import com.austro.orchestrator.domain.model.CreditEvaluation;
import com.austro.orchestrator.domain.model.EvaluationStatus;
import com.austro.orchestrator.domain.model.RiskData;
import com.austro.orchestrator.application.exception.CedulaInvalidaException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;

@Slf4j
@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class EvaluateCreditUseCaseImpl implements EvaluateCreditUseCase {

    private final CreditEvaluationRepository repository;
    private final RiskServicePort riskServicePort;
    private final CedulaValidatorService cedulaValidator;
    private final CreditApprovalService approvalService;

    @Override
    @Transactional
    public CreditEvaluation evaluate(String cedula, BigDecimal requestedAmount, int years, BigDecimal salary) {
        log.info("Iniciando evaluación de crédito para cédula: {}", cedula);

        if (!cedulaValidator.isValid(cedula)) {
            log.warn("Cédula inválida recibida: {}", cedula);
            throw new CedulaInvalidaException(cedula);
        }

        log.info("Consultando datos de riesgo para: {}", cedula);
        RiskData riskData = riskServicePort.getRiskData(cedula);
        log.info("Score de riesgo obtenido: {}, Deudas: {}", riskData.score(), riskData.debts().size());

        EvaluationStatus status = approvalService.evaluate(requestedAmount, years, salary, riskData);
        log.info("Resultado de evaluación para {}: {}", cedula, status);

        CreditEvaluation evaluation = CreditEvaluation.create(cedula, requestedAmount, years, salary, status);
        return repository.save(evaluation);
    }
}
