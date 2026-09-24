package com.stakevault.betting.stats.domain.model;

import java.time.LocalDate;
import java.util.UUID;

public record StatisticsFilter(UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId, UUID tipsterId,
		LocalDate from, LocalDate to) {

	private static final StatisticsFilter NONE = new StatisticsFilter(null, null, null, null, null, null, null);

	public static StatisticsFilter none() {
		return NONE;
	}

	public boolean isEmpty() {
		return this.equals(NONE);
	}
}
