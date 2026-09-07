package com.stakevault.betting.stats.domain.model;

// Serie historica para graficos de linha (RF10) - um bucket bruto por mes com apostas
// liquidadas, agrupado via DimDate.year/DimDate.month (ver FactBetRepository.aggregateByMonth).
public record MonthlyBetAggregate(int year, int month, BetAggregate aggregate) {
}
