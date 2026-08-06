package com.austro.risk.domain.model;

import java.util.List;

public record CustomerDebtsResult(List<CustomerDebt> debts) {

    public CustomerDebtsResult {
        debts = List.copyOf(debts);
    }
}
