package com.stakevault.betting.stats.domain.port.in;

import java.util.List;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;

public interface CalculateMetricsUseCase {

	BetMetrics calculateOverall(StatisticsFilter filter);

	List<SegmentedBetMetrics> calculateBySport(StatisticsFilter filter);

	List<SegmentedBetMetrics> calculateByMarket(StatisticsFilter filter);

	List<SegmentedBetMetrics> calculateByBettingHouse(StatisticsFilter filter);

	List<MonthlyBetMetrics> calculateMonthly(StatisticsFilter filter);
}
