package com.stakevault.betting.stats.domain.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

// RF09 estendido (epic-011): sportId/leagueId sao obrigatorios por contrato deste endpoint (ver
// GET /api/v1/statistics/search, docs/API-CONTRACTS.md) - diferente de StatisticsFilter (RF11,
// todos os campos opcionais, dashboard consolidado). A validacao HTTP (400 RFC 7807 se ausentes,
// MissingRequiredStatisticsFilterException) acontece antes deste record ser construido - o
// requireNonNull aqui e defesa em profundidade, nao o unico ponto de validacao.
public record StatisticsSearchFilter(UUID sportId, UUID leagueId, UUID teamId, UUID bettingHouseId, UUID marketId,
		UUID tipsterId, LocalDate from, LocalDate to) {

	public StatisticsSearchFilter {
		Objects.requireNonNull(sportId, "sportId");
		Objects.requireNonNull(leagueId, "leagueId");
	}
}
