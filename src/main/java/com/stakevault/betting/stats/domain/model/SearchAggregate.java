package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record SearchAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long lostCount, long settledCount,
		BigDecimal avgOdd) {
}
