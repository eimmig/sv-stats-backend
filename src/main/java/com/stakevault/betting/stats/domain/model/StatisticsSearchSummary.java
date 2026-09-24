package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record StatisticsSearchSummary(long betCount, BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi,
		BigDecimal winRate, BigDecimal avgOdd, BigDecimal maxDrawdown, BigDecimal sharpeRatio) {
}
