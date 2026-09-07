package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

public record SegmentedBetMetrics(UUID dimensionId, String dimensionName, BetMetrics metrics) {
}
