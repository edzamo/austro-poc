package com.austro.orchestrator.application.usecase;

import com.austro.orchestrator.application.port.in.GetEvaluationsUseCase;
import com.austro.orchestrator.application.port.out.CreditEvaluationRepository;
import com.austro.orchestrator.domain.model.CreditEvaluation;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.RequiredArgsConstructor;

import java.util.List;

@ApplicationScoped
@RequiredArgsConstructor(onConstructor_ = @Inject)
public class GetEvaluationsUseCaseImpl implements GetEvaluationsUseCase {

    private final CreditEvaluationRepository repository;

    @Override
    public List<CreditEvaluation> findAll() {
        return repository.listAllEvaluations();
    }
}
