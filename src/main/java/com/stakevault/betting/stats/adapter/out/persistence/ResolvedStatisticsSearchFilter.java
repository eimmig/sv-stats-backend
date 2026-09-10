package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.UUID;

// Mesmo padrao de ResolvedStatisticsFilter (parametro SpEL unico, evita java:S107) - sportId/
// leagueId nunca null aqui (StatisticsSearchFilter garante via requireNonNull), teamId opcional
// (team1Id OR team2Id). from/to ja vem com os limites-sentinela aplicados (ver JpaFactBetRepository).
record ResolvedStatisticsSearchFilter(UUID sportId, UUID leagueId, UUID teamId, UUID bettingHouseId, UUID marketId,
		UUID tipsterId, LocalDate from, LocalDate to) {
}
