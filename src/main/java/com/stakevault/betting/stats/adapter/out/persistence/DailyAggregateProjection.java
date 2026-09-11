package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

interface DailyAggregateProjection {

	LocalDate getDate();

	BigDecimal getTotalStaked();

	BigDecimal getNetProfit();

	Long getBetCount();
}
