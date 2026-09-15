package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Resposta de GET /api/v1/statistics/daily (epic-016) - shape enxuto (docs/API-CONTRACTS.md),
// distinto de BetMetrics (que carrega wonCount/lostCount/avgOdd/etc, nao expostos aqui).
public record DailyBetMetrics(LocalDate date, BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi,
		long betCount) {
}
