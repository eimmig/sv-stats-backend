package com.stakevault.betting.stats.application;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetDashboardMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetStatisticsDashboardUseCase;

// RF11/RN08: filtro presente bypassa o cache de feat-005 (as chaves so cobrem a vista sem
// filtro nenhum por tenant) e calcula direto - so a requisicao sem filtro nenhum usa o cache
// (RNF03, meta de performance com cache quente). "monthly" e sempre calculado direto (nunca via
// GetDashboardMetricsUseCase.getMonthly), decisao registrada no plan_review de feat-006.
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
					calculateMetrics.calculateMonthly(filter), dashboardMetrics.getByBetType());
		}
		return new StatisticsDashboard(calculateMetrics.calculateOverall(filter),
				calculateMetrics.calculateBySport(filter), calculateMetrics.calculateByMarket(filter),
				calculateMetrics.calculateByBettingHouse(filter), calculateMetrics.calculateMonthly(filter),
				calculateMetrics.calculateByBetType(filter));
	}
}
