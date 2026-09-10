package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;

interface AggregateWithOddProjection {

	BigDecimal getTotalStaked();

	BigDecimal getNetProfit();

	Long getWonCount();

	Long getSettledCount();

	BigDecimal getAvgOdd();
}
