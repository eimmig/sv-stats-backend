package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBetMetrics(LocalDate date, BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi,
		long betCount) {
}
