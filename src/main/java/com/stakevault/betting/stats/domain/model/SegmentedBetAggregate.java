package com.stakevault.betting.stats.domain.model;

public record SegmentedBetAggregate(String dimensionId, String dimensionName, BetAggregate aggregate) {
}
