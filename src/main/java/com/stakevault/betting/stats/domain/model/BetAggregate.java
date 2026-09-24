package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record BetAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long lostCount,
		long voidCount, long preCount, long liveCount, BigDecimal avgOdd, long settledCount) {
}
