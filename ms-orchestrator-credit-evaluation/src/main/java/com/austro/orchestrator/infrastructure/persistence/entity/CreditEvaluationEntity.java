package com.austro.orchestrator.infrastructure.persistence.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "credit_evaluations")
public class CreditEvaluationEntity extends PanacheEntityBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    public Long id;

    @Column(name = "cedula", nullable = false, length = 10)
    public String cedula;

    @Column(name = "requested_amount", nullable = false, precision = 15, scale = 2)
    public BigDecimal requestedAmount;

    @Column(name = "years", nullable = false)
    public int years;

    @Column(name = "salary", nullable = false, precision = 15, scale = 2)
    public BigDecimal salary;

    @Column(name = "final_status", nullable = false, length = 10)
    public String finalStatus;

    @Column(name = "evaluation_date", nullable = false)
    public LocalDateTime evaluationDate;
}
