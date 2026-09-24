package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetType;

record ResolvedStatisticsSearchFilter(UUID sportId, UUID leagueId, UUID teamId, UUID bettingHouseId, UUID marketId,
		UUID tipsterId, LocalDate from, LocalDate to, BetType betType) {
}
