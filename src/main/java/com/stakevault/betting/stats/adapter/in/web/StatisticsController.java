package com.stakevault.betting.stats.adapter.in.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stakevault.betting.stats.domain.model.BetType;
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

	@GetMapping("/search")
	public StatisticsSearchResult search(@RequestParam(required = false) UUID sportId,
			@RequestParam(required = false) UUID leagueId, @RequestParam(required = false) UUID teamId,
			@RequestParam(required = false) UUID bettingHouseId, @RequestParam(required = false) UUID marketId,
			@RequestParam(required = false) UUID tipsterId, @RequestParam(required = false) LocalDate from,
			@RequestParam(required = false) LocalDate to, @RequestParam(required = false) BetType betType) {
		if (sportId == null) {
			throw new MissingRequiredStatisticsFilterException("sportId");
		}
		if (leagueId == null) {
			throw new MissingRequiredStatisticsFilterException("leagueId");
		}
		StatisticsSearchFilter filter = new StatisticsSearchFilter(sportId, leagueId, teamId, bettingHouseId,
				marketId, tipsterId, from, to, betType);
		return searchStatistics.search(filter);
	}

	@GetMapping("/daily")
	public List<DailyBetMetrics> daily(@RequestParam(required = false) UUID bettingHouseId,
			@RequestParam(required = false) UUID sportId, @RequestParam(required = false) UUID leagueId,
			@RequestParam(required = false) UUID marketId, @RequestParam(required = false) UUID tipsterId,
			@RequestParam(required = false) LocalDate from, @RequestParam(required = false) LocalDate to) {
		StatisticsFilter filter = new StatisticsFilter(bettingHouseId, sportId, leagueId, marketId, tipsterId, from,
				to);
		return calculateMetrics.calculateDaily(filter);
	}

	@GetMapping("/teams")
	public List<DimTeam> teams(@RequestParam(required = false) UUID sportId) {
		if (sportId == null) {
			throw new MissingRequiredStatisticsFilterException("sportId");
		}
		return listTeams.listBySport(sportId);
	}
}
