package com.stakevault.betting.stats.domain.model;

// dimensionId e String desde epic-014 (byBetType usa "PRE"/"LIVE", nao uuid) - ver
// SegmentedBetAggregate.
public record SegmentedBetMetrics(String dimensionId, String dimensionName, BetMetrics metrics) {
}
