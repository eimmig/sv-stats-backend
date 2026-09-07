package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.UUID;

// Agrupa os 7 campos de filtro num unico parametro de @Query (SpEL, "#filter.xxx()") em vez de
// um por metodo - achado real do SonarCloud (java:S107, mais de 7 parametros) na primeira
// tentativa. from/to aqui ja vieram com os limites-sentinela aplicados (ver
// JpaFactBetRepository), nunca null - diferente do StatisticsFilter de dominio.
record ResolvedStatisticsFilter(UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId, UUID tipsterId,
		LocalDate from, LocalDate to) {
}
