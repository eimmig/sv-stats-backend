package com.stakevault.betting.stats.domain.port.in;

import java.util.List;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;

public interface CalculateMetricsUseCase {

	BetMetrics calculateOverall();

	List<SegmentedBetMetrics> calculateBySport();

	List<SegmentedBetMetrics> calculateByMarket();

	List<SegmentedBetMetrics> calculateByBettingHouse();
}
