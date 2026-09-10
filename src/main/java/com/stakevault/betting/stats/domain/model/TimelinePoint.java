package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Um ponto da equity curve exposta em GET /api/v1/statistics/search ("timeline") - cumulativeProfit
// e o lucro acumulado ate aquela data (mesma serie usada para calcular maxDrawdown em
// EquityCurveCalculator, ver docs/STATISTICS.md).
public record TimelinePoint(LocalDate date, BigDecimal cumulativeProfit) {
}
