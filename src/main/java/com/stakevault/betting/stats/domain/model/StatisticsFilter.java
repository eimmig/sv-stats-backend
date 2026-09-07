package com.stakevault.betting.stats.domain.model;

import java.time.LocalDate;
import java.util.UUID;

// RF11/RN08: filtro dinamico aplicado a FACT_BET. Todos os campos sao opcionais - none()
// representa "sem filtro nenhum" (unica combinacao que usa o cache de feat-005, ver
// GetDashboardMetricsUseCase/StatisticsController).
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
