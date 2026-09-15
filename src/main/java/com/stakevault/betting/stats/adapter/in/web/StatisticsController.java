package com.stakevault.betting.stats.adapter.in.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stakevault.betting.stats.domain.model.DailyBetMetrics;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.model.MissingRequiredStatisticsFilterException;
import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchResult;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetStatisticsDashboardUseCase;
import com.stakevault.betting.stats.domain.port.in.ListTeamsUseCase;
import com.stakevault.betting.stats.domain.port.in.SearchStatisticsUseCase;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

	private final GetStatisticsDashboardUseCase getStatisticsDashboard;
	private final SearchStatisticsUseCase searchStatistics;
	private final ListTeamsUseCase listTeams;
	private final CalculateMetricsUseCase calculateMetrics;

	public StatisticsController(GetStatisticsDashboardUseCase getStatisticsDashboard,
			SearchStatisticsUseCase searchStatistics, ListTeamsUseCase listTeams,
			CalculateMetricsUseCase calculateMetrics) {
		this.getStatisticsDashboard = getStatisticsDashboard;
		this.searchStatistics = searchStatistics;
		this.listTeams = listTeams;
		this.calculateMetrics = calculateMetrics;
	}

	@GetMapping
	public StatisticsDashboard get(@RequestParam(required = false) UUID bettingHouseId,
			@RequestParam(required = false) UUID sportId, @RequestParam(required = false) UUID leagueId,
			@RequestParam(required = false) UUID marketId, @RequestParam(required = false) UUID tipsterId,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
		StatisticsFilter filter = new StatisticsFilter(bettingHouseId, sportId, leagueId, marketId, tipsterId, from,
				to);
		return getStatisticsDashboard.getDashboard(filter);
	}

	// epic-011: sportId/leagueId obrigatorios (400 RFC 7807 localizado se ausentes) - unico ponto
	// do contrato de estatisticas onde um filtro deixa de ser opcional. required=false aqui de
	// proposito (ver plan_review de feat-012) - a validacao manual abaixo garante o formato de
	// erro do servico (LocalizedDomainException/DomainExceptionHandler) em vez do 400 generico
	// que o Spring MVC devolveria para um @RequestParam(required = true) ausente.
	@GetMapping("/search")
	public StatisticsSearchResult search(@RequestParam(required = false) UUID sportId,
			@RequestParam(required = false) UUID leagueId, @RequestParam(required = false) UUID teamId,
			@RequestParam(required = false) UUID bettingHouseId, @RequestParam(required = false) UUID marketId,
			@RequestParam(required = false) UUID tipsterId, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to) {
		if (sportId == null) {
			throw new MissingRequiredStatisticsFilterException("sportId");
		}
		if (leagueId == null) {
			throw new MissingRequiredStatisticsFilterException("leagueId");
		}
		StatisticsSearchFilter filter = new StatisticsSearchFilter(sportId, leagueId, teamId, bettingHouseId,
				marketId, tipsterId, from, to);
		return searchStatistics.search(filter);
	}

	// epic-016: mesmos 7 filtros opcionais do bundle consolidado, granularidade diaria - array
	// esparso (so dias com pelo menos 1 aposta liquidada), sem cache-aside (sempre calculado
	// direto, mesmo sem filtro nenhum - shape de lista por dia nao bate com a chave unica do
	// cache-aside de GetStatisticsDashboardService).
	@GetMapping("/daily")
	public List<DailyBetMetrics> daily(@RequestParam(required = false) UUID bettingHouseId,
			@RequestParam(required = false) UUID sportId, @RequestParam(required = false) UUID leagueId,
			@RequestParam(required = false) UUID marketId, @RequestParam(required = false) UUID tipsterId,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
		StatisticsFilter filter = new StatisticsFilter(bettingHouseId, sportId, leagueId, marketId, tipsterId, from,
				to);
		return calculateMetrics.calculateDaily(filter);
	}

	// feat-013: DIM_TEAM nao tem catalogo em bets-service (texto livre por aposta) - autocomplete
	// da tela "Buscar Estatisticas" precisa desta listagem pra oferecer os times ja vistos.
	// sportId obrigatorio (mesmo padrao de erro do endpoint acima) porque a chave natural de
	// DIM_TEAM e composta (name, sportId) - trocar de esporte na UI refiltra a lista.
	@GetMapping("/teams")
	public List<DimTeam> teams(@RequestParam(required = false) UUID sportId) {
		if (sportId == null) {
			throw new MissingRequiredStatisticsFilterException("sportId");
		}
		return listTeams.listBySport(sportId);
	}
}
