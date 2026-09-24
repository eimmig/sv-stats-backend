package com.stakevault.betting.stats.domain.model;

public record MonthlyBetAggregate(int year, int month, BetAggregate aggregate) {
}
