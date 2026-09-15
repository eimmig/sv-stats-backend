package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;

interface AggregateProjection {

	BigDecimal getTotalStaked();

	BigDecimal getNetProfit();

	Long getWonCount();

	Long getLostCount();

	Long getVoidCount();

	Long getPreCount();

	Long getLiveCount();

	BigDecimal getAvgOdd();

	Long getSettledCount();
}
