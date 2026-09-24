package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record BetMetrics(BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi, BigDecimal winRate,
		long settledCount, long wonCount, long lostCount, long voidCount, long preCount, long liveCount,
		BigDecimal avgOdd) {
}
