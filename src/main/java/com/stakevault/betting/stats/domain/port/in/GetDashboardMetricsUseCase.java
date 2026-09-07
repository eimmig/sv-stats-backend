package com.stakevault.betting.stats.domain.port.in;

import java.util.List;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;

// Cache-aside (feat-005): hit no MetricsCacheRepository responde direto; miss calcula via
// CalculateMetricsUseCase e grava no cache antes de retornar.
public interface GetDashboardMetricsUseCase {

	BetMetrics getOverall();

	List<SegmentedBetMetrics> getBySport();

	List<SegmentedBetMetrics> getByMarket();

	List<SegmentedBetMetrics> getByBettingHouse();

	BetMetrics getMonthly(int year, int month);
}
