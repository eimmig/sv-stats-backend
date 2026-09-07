package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

@ExtendWith(MockitoExtension.class)
class GetDashboardMetricsServiceTest {

	@Mock
	private CalculateMetricsUseCase calculateMetrics;
	@Mock
	private MetricsCacheRepository cache;

	private GetDashboardMetricsService service;

	@BeforeEach
	void setUp() {
		service = new GetDashboardMetricsService(calculateMetrics, cache);
	}

	private BetMetrics sampleMetrics() {
		return new BetMetrics(BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(0.5),
				BigDecimal.valueOf(0.5), 1);
	}

	@Test
	void shouldReturnCachedOverallWithoutCallingCalculateOnHit() {
		when(cache.findOverall()).thenReturn(Optional.of(sampleMetrics()));

		BetMetrics result = service.getOverall();

		assertThat(result).isEqualTo(sampleMetrics());
		verify(calculateMetrics, never()).calculateOverall();
	}

	@Test
	void shouldCalculateAndSaveOverallOnMiss() {
		when(cache.findOverall()).thenReturn(Optional.empty());
		when(calculateMetrics.calculateOverall()).thenReturn(sampleMetrics());

		BetMetrics result = service.getOverall();

		assertThat(result).isEqualTo(sampleMetrics());
		verify(cache).saveOverall(sampleMetrics());
	}

	@Test
	void shouldReturnCachedSegmentWithoutCallingCalculateOnHit() {
		List<SegmentedBetMetrics> segments = List.of(new SegmentedBetMetrics(UUID.randomUUID(), "Soccer",
				sampleMetrics()));
		when(cache.findBySport()).thenReturn(Optional.of(segments));

		List<SegmentedBetMetrics> result = service.getBySport();

		assertThat(result).isEqualTo(segments);
		verify(calculateMetrics, never()).calculateBySport();
	}

	@Test
	void shouldCalculateAndSaveSegmentOnMiss() {
		List<SegmentedBetMetrics> segments = List.of(new SegmentedBetMetrics(UUID.randomUUID(), "Soccer",
				sampleMetrics()));
		when(cache.findBySport()).thenReturn(Optional.empty());
		when(calculateMetrics.calculateBySport()).thenReturn(segments);

		List<SegmentedBetMetrics> result = service.getBySport();

		assertThat(result).isEqualTo(segments);
		verify(cache).saveBySport(segments);
	}

	@Test
	void shouldReturnCachedMonthlyWithoutCallingCalculateOnHit() {
		when(cache.findMonthly(2026, 9)).thenReturn(Optional.of(sampleMetrics()));

		BetMetrics result = service.getMonthly(2026, 9);

		assertThat(result).isEqualTo(sampleMetrics());
		verify(calculateMetrics, never()).calculateMonthly();
	}

	@Test
	void shouldCalculateWholeSeriesAndSaveEachMonthOnMonthlyMiss() {
		when(cache.findMonthly(2026, 9)).thenReturn(Optional.empty());
		when(calculateMetrics.calculateMonthly()).thenReturn(
				List.of(new MonthlyBetMetrics(2026, 8, sampleMetrics()), new MonthlyBetMetrics(2026, 9, sampleMetrics())));

		BetMetrics result = service.getMonthly(2026, 9);

		assertThat(result).isEqualTo(sampleMetrics());
		verify(cache).saveMonthly(2026, 8, sampleMetrics());
		verify(cache).saveMonthly(2026, 9, sampleMetrics());
	}

	@Test
	void shouldReturnZeroMetricsWhenRequestedMonthHasNoData() {
		when(cache.findMonthly(2026, 1)).thenReturn(Optional.empty());
		when(calculateMetrics.calculateMonthly()).thenReturn(List.of());

		BetMetrics result = service.getMonthly(2026, 1);

		assertThat(result.totalStaked()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(result.settledCount()).isZero();
	}

	@Test
	void shouldCacheZeroMetricsForAMonthWithNoDataToAvoidRecomputingOnEveryRequest() {
		when(cache.findMonthly(2026, 1)).thenReturn(Optional.empty());
		when(calculateMetrics.calculateMonthly()).thenReturn(List.of());

		service.getMonthly(2026, 1);

		verify(cache).saveMonthly(2026, 1, new BetMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, 0));
	}
}
