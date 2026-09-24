package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.UUID;

record ResolvedStatisticsFilter(UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId, UUID tipsterId,
		LocalDate from, LocalDate to) {
}
