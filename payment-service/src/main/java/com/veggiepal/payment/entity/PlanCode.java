package com.veggiepal.payment.entity;

public enum PlanCode {
    FREE(0), PLUS(1), PRO(2);

    private final int rank;
    PlanCode(int rank) { this.rank = rank; }
    public int rank() { return rank; }
}
