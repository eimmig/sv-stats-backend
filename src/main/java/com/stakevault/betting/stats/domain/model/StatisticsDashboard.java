package com.stakevault.betting.stats.domain.model;

import java.util.List;

public record StatisticsDashboard(BetMetrics overall, List<SegmentedBetMetrics> bySport,
		List<SegmentedBetMetrics> byMarket, List<SegmentedBetMetrics> byBettingHouse,
		List<MonthlyBetMetrics> monthly, List<SegmentedBetMetrics> byBetType,
		List<SegmentedBetMetrics> byLeague, List<SegmentedBetMetrics> byTipster,
		List<SegmentedBetMetrics> byTeam) {
}
