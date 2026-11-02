package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;

interface AggregateWithOddProjection {

	BigDecimal getTotalStaked();

	BigDecimal getNetProfit();

	Long getWonCount();

	Long getLostCount();

	Long getSettledCount();

	BigDecimal getAvgOdd();
}
