package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

interface SegmentedAggregateProjection extends AggregateProjection {

	UUID getDimensionId();

	String getDimensionName();
}
