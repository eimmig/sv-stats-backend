package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetDashboardMetricsUseCase;

@ExtendWith(MockitoExtension.class)
class GetStatisticsDashboardServiceTest {

	@Mock
	private GetDashboardMetricsUseCase dashboardMetrics;
	@Mock
	private CalculateMetricsUseCase calculateMetrics;

	private GetStatisticsDashboardService service;

	@BeforeEach
	void setUp() {
		service = new GetStatisticsDashboardService(dashboardMetrics, calculateMetrics);
	}

	private BetMetrics sampleMetrics() {
		return new BetMetrics(BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(0.5),
				BigDecimal.valueOf(0.5), 1, 1, 0, 0, 0, 0, BigDecimal.valueOf(1.92), null, null, null, null, null, null);
	}

	@Test
	void shouldServeTheUnfilteredMonthlySeriesFromTheCachedUseCase() {
		List<MonthlyBetMetrics> series = List.of(new MonthlyBetMetrics(2026, 9, sampleMetrics()));
		when(dashboardMetrics.getMonthlySeries()).thenReturn(series);

		StatisticsDashboard dashboard = service.getDashboard(StatisticsFilter.none());

		assertThat(dashboard.monthly()).isEqualTo(series);
		verify(calculateMetrics, never()).calculateMonthly(any());
	}
}
