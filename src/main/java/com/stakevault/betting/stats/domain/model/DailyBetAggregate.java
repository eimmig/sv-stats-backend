package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// GET /api/v1/statistics/daily (epic-016) - um bucket bruto por dia com apostas liquidadas,
// agrupado via DimDate.day/month/year (ver FactBetRepository.aggregateByDay). Array esparso: dias
// sem aposta liquidada nao geram linha.
public record DailyBetAggregate(LocalDate date, BigDecimal totalStaked, BigDecimal netProfit, long betCount) {
}
