package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.DailyBetAggregate;
import com.stakevault.betting.stats.domain.model.DailyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;

@ExtendWith(MockitoExtension.class)
class CalculateMetricsServiceTest {

	@Mock
	private FactBetRepository factBetRepository;

	private CalculateMetricsService service;

	@BeforeEach
	void setUp() {
		service = new CalculateMetricsService(factBetRepository);
	}

	@ParameterizedTest(name = "staked={0} profit={1} won={2} lost={3} void={4} -> roi={5} winRate={6}")
	@CsvSource({
			"200, -50, 1, 1, 0, -0.2500, 0.5000",
			"0, 0, 0, 0, 0, 0.0000, 0.0000",
			"100, 100, 1, 0, 0, 1.0000, 1.0000",
			"100, -100, 0, 1, 0, -1.0000, 0.0000",
			"300, 50, 1, 1, 1, 0.1667, 0.5000",
			"50, 0, 0, 0, 1, 0.0000, 0.0000" })
	void shouldComputeRoiAndWinRateFromAggregate(String staked, String profit, long won, long lost, long voided,
			String roi, String winRate) {
		long settled = won + lost + voided;
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(new BetAggregate(
				new BigDecimal(staked), new BigDecimal(profit), won, lost, voided, 0, 0, null, settled, null,
				BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.roi()).isEqualByComparingTo(new BigDecimal(roi));
		assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal(winRate));
		assertThat(metrics.settledCount()).isEqualTo(settled);
	}

	@Test
	void shouldComputeImpliedProbabilityEdgeAndConfidenceIntervals() {
		// 2 ganhas, 1 perdida e 1 devolvida: stakes 100, 50, 30, 20 e lucros 100, -50, 15, 0
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(new BetAggregate(
				BigDecimal.valueOf(200), BigDecimal.valueOf(65), 2, 1, 1, 4, 0, BigDecimal.valueOf(2.15), 4,
				new BigDecimal("0.5625"), new BigDecimal("12725"), new BigDecimal("13800"), new BigDecimal("7950")));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.winRate()).isEqualByComparingTo("0.6667");
		assertThat(metrics.impliedProbability()).isEqualByComparingTo("0.5625");
		assertThat(metrics.edge()).isEqualByComparingTo("0.1042");
		assertThat(metrics.winRateLower()).isEqualByComparingTo("0.2077");
		assertThat(metrics.winRateUpper()).isEqualByComparingTo("0.9385");
		assertThat(metrics.roi()).isEqualByComparingTo("0.3250");
		assertThat(metrics.roiLower()).isEqualByComparingTo("-0.7494");
		assertThat(metrics.roiUpper()).isEqualByComparingTo("1.3994");
	}

	@Test
	void shouldLeaveImpliedProbabilityEdgeAndIntervalsNullWithoutDecidedBets() {
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(new BetAggregate(
				BigDecimal.valueOf(50), BigDecimal.ZERO, 0, 0, 1, 0, 0, null, 1, null, BigDecimal.ZERO,
				new BigDecimal("2500"), BigDecimal.ZERO));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.impliedProbability()).isNull();
		assertThat(metrics.edge()).isNull();
		assertThat(metrics.winRateLower()).isNull();
		assertThat(metrics.winRateUpper()).isNull();
		assertThat(metrics.roiLower()).isNull();
		assertThat(metrics.roiUpper()).isNull();
	}

	@Test
	void shouldCopyNewCountsAndAvgOddFromAggregateToMetrics() {
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(
				new BetAggregate(BigDecimal.valueOf(300), BigDecimal.valueOf(-50), 1, 1, 1, 1, 1,
						BigDecimal.valueOf(1.75), 3, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.wonCount()).isEqualTo(1);
		assertThat(metrics.lostCount()).isEqualTo(1);
		assertThat(metrics.voidCount()).isEqualTo(1);
		assertThat(metrics.preCount()).isEqualTo(1);
		assertThat(metrics.liveCount()).isEqualTo(1);
		assertThat(metrics.avgOdd()).isEqualByComparingTo(BigDecimal.valueOf(1.75));
	}

	@Test
	void shouldMapSegmentedAggregatesPreservingDimensionIdentity() {
		String sportId = UUID.randomUUID().toString();
		when(factBetRepository.aggregateBySport(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate(sportId, "Soccer",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));

		List<SegmentedBetMetrics> result = service.calculateBySport(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		SegmentedBetMetrics segment = result.get(0);
		assertThat(segment.dimensionId()).isEqualTo(sportId);
		assertThat(segment.dimensionName()).isEqualTo("Soccer");
		assertThat(segment.metrics().roi()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
		assertThat(segment.metrics().winRate()).isEqualByComparingTo(BigDecimal.ONE);
	}

	@Test
	void shouldMapByLeagueSegmentFromTheLeagueAggregate() {
		String leagueId = UUID.randomUUID().toString();
		when(factBetRepository.aggregateByLeague(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate(leagueId, "Premier League",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));

		List<SegmentedBetMetrics> result = service.calculateByLeague(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).dimensionId()).isEqualTo(leagueId);
		assertThat(result.get(0).dimensionName()).isEqualTo("Premier League");
	}

	@Test
	void shouldMapByTipsterSegmentFromTheTipsterAggregate() {
		String tipsterId = UUID.randomUUID().toString();
		when(factBetRepository.aggregateByTipster(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate(tipsterId, "Tipster",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));

		List<SegmentedBetMetrics> result = service.calculateByTipster(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).dimensionId()).isEqualTo(tipsterId);
		assertThat(result.get(0).dimensionName()).isEqualTo("Tipster");
	}

	@Test
	void shouldMapByBetTypeSegmentUsingEnumNameAsDimensionId() {
		when(factBetRepository.aggregateByBetType(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate("PRE", "PRE",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 1, 0,
								BigDecimal.valueOf(1.9), 1, null, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO))));

		List<SegmentedBetMetrics> result = service.calculateByBetType(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).dimensionId()).isEqualTo("PRE");
		assertThat(result.get(0).dimensionName()).isEqualTo("PRE");
	}

	@Test
	void shouldComputeDailyRoiReusingTheSameZeroSafeDivision() {
		java.time.LocalDate day = java.time.LocalDate.of(2026, 9, 6);
		when(factBetRepository.aggregateByDay(StatisticsFilter.none())).thenReturn(List.of(
				new DailyBetAggregate(day, BigDecimal.valueOf(200), BigDecimal.valueOf(-50), 2),
				new DailyBetAggregate(day.plusDays(1), BigDecimal.ZERO, BigDecimal.ZERO, 0)));

		List<DailyBetMetrics> result = service.calculateDaily(StatisticsFilter.none());

		assertThat(result).hasSize(2);
		assertThat(result.get(0).date()).isEqualTo(day);
		assertThat(result.get(0).roi()).isEqualByComparingTo(BigDecimal.valueOf(-0.25));
		assertThat(result.get(0).betCount()).isEqualTo(2);
		assertThat(result.get(1).roi()).isEqualByComparingTo(BigDecimal.ZERO);
	}
}
