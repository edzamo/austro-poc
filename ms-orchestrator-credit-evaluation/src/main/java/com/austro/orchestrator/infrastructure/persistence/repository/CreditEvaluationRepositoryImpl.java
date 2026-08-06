package com.austro.orchestrator.infrastructure.persistence.repository;

import com.austro.orchestrator.application.port.out.CreditEvaluationRepository;
import com.austro.orchestrator.domain.model.CreditEvaluation;
import com.austro.orchestrator.domain.model.EvaluationStatus;
import com.austro.orchestrator.infrastructure.persistence.entity.CreditEvaluationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

/**
 * Adaptador de salida: implementa el puerto de repositorio usando Panache.
 * Convierte entre el record de dominio (CreditEvaluation) y la entidad JPA.
 */
@ApplicationScoped
public class CreditEvaluationRepositoryImpl
        implements CreditEvaluationRepository, PanacheRepository<CreditEvaluationEntity> {

    @Override
    public CreditEvaluation save(CreditEvaluation evaluation) {
        CreditEvaluationEntity entity = toEntity(evaluation);
        persist(entity);
        return toDomain(entity);
    }

    @Override
    public List<CreditEvaluation> listAllEvaluations() {
        return listAll().stream()
                .map(this::toDomain)
                .toList();
    }

    private CreditEvaluationEntity toEntity(CreditEvaluation evaluation) {
        CreditEvaluationEntity entity = new CreditEvaluationEntity();
        entity.cedula = evaluation.cedula();
        entity.requestedAmount = evaluation.requestedAmount();
        entity.years = evaluation.years();
        entity.salary = evaluation.salary();
        entity.finalStatus = evaluation.finalStatus().name();
        entity.evaluationDate = evaluation.evaluationDate();
        return entity;
    }

    private CreditEvaluation toDomain(CreditEvaluationEntity entity) {
        return CreditEvaluation.reconstitute(
                entity.id,
                entity.cedula,
                entity.requestedAmount,
                entity.years,
                entity.salary,
                EvaluationStatus.valueOf(entity.finalStatus),
                entity.evaluationDate
        );
    }
}
