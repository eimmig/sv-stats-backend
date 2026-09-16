package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// RN04 (roi/winRate, mesma formula de BetMetrics) + avgOdd/maxDrawdown/sharpeRatio (formulas em
// docs/STATISTICS.md). roi/winRate ZERO (nao excecao) quando o denominador e zero, mesma
// decisao de CalculateMetricsService; sharpeRatio pode ser null.
public record StatisticsSearchSummary(long betCount, BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi,
		BigDecimal winRate, BigDecimal avgOdd, BigDecimal maxDrawdown, BigDecimal sharpeRatio) {
}
