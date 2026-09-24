package com.stakevault.betting.stats.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public record StatisticsSearchFilter(UUID sportId, UUID leagueId, UUID teamId, UUID bettingHouseId, UUID marketId,
		UUID tipsterId, LocalDate from, LocalDate to, BetType betType) {

	public StatisticsSearchFilter {
		Objects.requireNonNull(sportId, "sportId");
		Objects.requireNonNull(leagueId, "leagueId");
	}
}
