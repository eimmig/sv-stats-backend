package com.stakevault.betting.stats.application;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetDashboardMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetStatisticsDashboardUseCase;

@Service
public class GetStatisticsDashboardService implements GetStatisticsDashboardUseCase {

	private final GetDashboardMetricsUseCase dashboardMetrics;
	private final CalculateMetricsUseCase calculateMetrics;

	public GetStatisticsDashboardService(GetDashboardMetricsUseCase dashboardMetrics,
			CalculateMetricsUseCase calculateMetrics) {
		this.dashboardMetrics = dashboardMetrics;
		this.calculateMetrics = calculateMetrics;
	}

	@Override
	public StatisticsDashboard getDashboard(StatisticsFilter filter) {
		if (filter.isEmpty()) {
			return new StatisticsDashboard(dashboardMetrics.getOverall(), dashboardMetrics.getBySport(),
					dashboardMetrics.getByMarket(), dashboardMetrics.getByBettingHouse(),
					calculateMetrics.calculateMonthly(filter), dashboardMetrics.getByBetType(),
					dashboardMetrics.getByLeague(), dashboardMetrics.getByTipster(), dashboardMetrics.getByTeam());
		}
		return new StatisticsDashboard(calculateMetrics.calculateOverall(filter),
				calculateMetrics.calculateBySport(filter), calculateMetrics.calculateByMarket(filter),
				calculateMetrics.calculateByBettingHouse(filter), calculateMetrics.calculateMonthly(filter),
				calculateMetrics.calculateByBetType(filter), calculateMetrics.calculateByLeague(filter),
				calculateMetrics.calculateByTipster(filter), calculateMetrics.calculateByTeam(filter));
	}
}
