package com.stakevault.betting.stats.adapter.out.persistence;

import com.stakevault.betting.stats.domain.model.BetType;

interface BetTypeAggregateProjection extends AggregateProjection {

	BetType getBetType();
}
