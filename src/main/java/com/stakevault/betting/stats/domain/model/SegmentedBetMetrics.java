package com.stakevault.betting.stats.domain.model;

public record SegmentedBetMetrics(String dimensionId, String dimensionName, BetMetrics metrics) {
}
