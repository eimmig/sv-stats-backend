package com.stakevault.betting.stats.domain.model;

import java.util.List;

// RF11/RN08: bundle unico retornado por GET /api/v1/statistics - decisao do usuario (2026-09-07,
// AskUserQuestion) entre bundle unico/resposta minima+groupBy/endpoints separados por segmento.
public record StatisticsDashboard(BetMetrics overall, List<SegmentedBetMetrics> bySport,
		List<SegmentedBetMetrics> byMarket, List<SegmentedBetMetrics> byBettingHouse,
		List<MonthlyBetMetrics> monthly, List<SegmentedBetMetrics> byBetType,
		List<SegmentedBetMetrics> byLeague, List<SegmentedBetMetrics> byTipster) {
}
