package com.stakevault.betting.stats.application;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetDashboardMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetStatisticsDashboardUseCase;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

@Service
public class GetStatisticsDashboardService implements GetStatisticsDashboardUseCase {

	private final GetDashboardMetricsUseCase dashboardMetrics;
	private final CalculateMetricsUseCase calculateMetrics;
	private final ConcurrentCalls concurrentCalls;
	private final MetricsCacheRepository metricsCache;

	public GetStatisticsDashboardService(GetDashboardMetricsUseCase dashboardMetrics,
			CalculateMetricsUseCase calculateMetrics, ConcurrentCalls concurrentCalls,
			MetricsCacheRepository metricsCache) {
		this.dashboardMetrics = dashboardMetrics;
		this.calculateMetrics = calculateMetrics;
		this.concurrentCalls = concurrentCalls;
		this.metricsCache = metricsCache;
	}

	@Override
	public StatisticsDashboard getDashboard(StatisticsFilter filter) {
		if (filter.isEmpty()) {
			return new StatisticsDashboard(dashboardMetrics.getOverall(), dashboardMetrics.getBySport(),
					dashboardMetrics.getByMarket(), dashboardMetrics.getByBettingHouse(),
					dashboardMetrics.getMonthlySeries(), dashboardMetrics.getByBetType(),
					dashboardMetrics.getByLeague(), dashboardMetrics.getByTipster(), dashboardMetrics.getByTeam());
		}
		return metricsCache.findDashboard(filter).orElseGet(() -> {
			StatisticsDashboard dashboard = calculateFiltered(filter);
			metricsCache.saveDashboard(filter, dashboard);
			return dashboard;
		});
	}

	// as nove agregacoes sao independentes e rodam ao mesmo tempo
	private StatisticsDashboard calculateFiltered(StatisticsFilter filter) {
		var overall = concurrentCalls.fork(() -> calculateMetrics.calculateOverall(filter));
		var bySport = concurrentCalls.fork(() -> calculateMetrics.calculateBySport(filter));
		var byMarket = concurrentCalls.fork(() -> calculateMetrics.calculateByMarket(filter));
		var byHouse = concurrentCalls.fork(() -> calculateMetrics.calculateByBettingHouse(filter));
		var monthly = concurrentCalls.fork(() -> calculateMetrics.calculateMonthly(filter));
		var byBetType = concurrentCalls.fork(() -> calculateMetrics.calculateByBetType(filter));
		var byLeague = concurrentCalls.fork(() -> calculateMetrics.calculateByLeague(filter));
		var byTipster = concurrentCalls.fork(() -> calculateMetrics.calculateByTipster(filter));
		var byTeam = concurrentCalls.fork(() -> calculateMetrics.calculateByTeam(filter));
		return new StatisticsDashboard(overall.get(), bySport.get(), byMarket.get(), byHouse.get(), monthly.get(),
				byBetType.get(), byLeague.get(), byTipster.get(), byTeam.get());
	}
}
