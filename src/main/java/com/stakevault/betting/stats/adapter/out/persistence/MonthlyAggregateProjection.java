package com.stakevault.betting.stats.adapter.out.persistence;

interface MonthlyAggregateProjection extends AggregateProjection {

	int getYear();

	int getMonth();
}
