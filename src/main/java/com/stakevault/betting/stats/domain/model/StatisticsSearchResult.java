package com.stakevault.betting.stats.domain.model;

import java.util.List;

// Resposta de GET /api/v1/statistics/search (ver docs/API-CONTRACTS.md) - filters ecoa o filtro
// resolvido (sportId/leagueId sempre presentes), timeline e a equity curve da combinacao.
public record StatisticsSearchResult(StatisticsSearchFilter filters, StatisticsSearchSummary summary,
		List<TimelinePoint> timeline) {
}
