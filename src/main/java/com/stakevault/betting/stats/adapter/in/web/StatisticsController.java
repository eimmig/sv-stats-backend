package com.stakevault.betting.stats.adapter.in.web;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.GetStatisticsDashboardUseCase;

@RestController
@RequestMapping("/api/v1/statistics")
public class StatisticsController {

	private final GetStatisticsDashboardUseCase getStatisticsDashboard;

	public StatisticsController(GetStatisticsDashboardUseCase getStatisticsDashboard) {
		this.getStatisticsDashboard = getStatisticsDashboard;
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
}
