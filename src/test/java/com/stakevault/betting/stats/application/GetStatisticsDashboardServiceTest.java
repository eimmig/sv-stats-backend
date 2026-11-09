package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;

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
		service = new GetStatisticsDashboardService(dashboardMetrics, calculateMetrics, new SameThreadCalls());
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

	@Test
	void shouldComputeEveryAggregateOfAFilteredDashboardThroughTheConcurrentCalls() {
		StatisticsFilter filter = new StatisticsFilter(null, null, null, null, null, LocalDate.of(2026, 8, 25),
				LocalDate.of(2026, 10, 25));
		List<MonthlyBetMetrics> series = List.of(new MonthlyBetMetrics(2026, 9, sampleMetrics()));
		when(calculateMetrics.calculateOverall(filter)).thenReturn(sampleMetrics());
		when(calculateMetrics.calculateMonthly(filter)).thenReturn(series);
		SameThreadCalls calls = new SameThreadCalls();
		service = new GetStatisticsDashboardService(dashboardMetrics, calculateMetrics, calls);

		StatisticsDashboard dashboard = service.getDashboard(filter);

		assertThat(dashboard.overall()).isEqualTo(sampleMetrics());
		assertThat(dashboard.monthly()).isEqualTo(series);
		assertThat(calls.forks).isEqualTo(9);
		verify(calculateMetrics).calculateBySport(filter);
		verify(calculateMetrics).calculateByMarket(filter);
		verify(calculateMetrics).calculateByBettingHouse(filter);
		verify(calculateMetrics).calculateByBetType(filter);
		verify(calculateMetrics).calculateByLeague(filter);
		verify(calculateMetrics).calculateByTipster(filter);
		verify(calculateMetrics).calculateByTeam(filter);
		verify(dashboardMetrics, never()).getOverall();
	}

	private static final class SameThreadCalls implements ConcurrentCalls {
		private int forks;

		@Override
		public <T> Supplier<T> fork(Supplier<T> call) {
			forks++;
			T value = call.get();
			return () -> value;
		}
	}
}
