package com.stakevault.betting.stats.domain.port.in;

import java.util.List;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;

public interface GetDashboardMetricsUseCase {

	BetMetrics getOverall();

	List<SegmentedBetMetrics> getBySport();

	List<SegmentedBetMetrics> getByMarket();

	List<SegmentedBetMetrics> getByBettingHouse();

	List<SegmentedBetMetrics> getByLeague();

	List<SegmentedBetMetrics> getByTipster();

	List<SegmentedBetMetrics> getByTeam();

	BetMetrics getMonthly(int year, int month);

	List<SegmentedBetMetrics> getByBetType();
}
