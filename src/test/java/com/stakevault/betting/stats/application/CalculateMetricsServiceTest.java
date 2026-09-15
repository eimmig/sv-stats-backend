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

	@ParameterizedTest(name = "staked={0} profit={1} won={2} settled={3} -> roi={4} winRate={5}")
	@CsvSource({
			// caso normal: 2 apostas de 100, uma ganha (+50), taxa de acerto 50%.
			"200, -50, 1, 2, -0.2500, 0.5000",
			// total investido zero (nenhuma aposta liquidada) - RN04 nao define, retorna ZERO.
			"0, 0, 0, 0, 0.0000, 0.0000",
			// liquidadas=0 mas totalStaked>0 nao acontece na pratica (settledCount conta a
			// propria agregacao), mas a formula de winRate e blindada de qualquer forma.
			"100, 100, 1, 1, 1.0000, 1.0000",
			// nenhuma vitoria.
			"100, -100, 0, 1, -1.0000, 0.0000" })
	void shouldComputeRoiAndWinRateFromAggregate(String staked, String profit, long won, long settled, String roi,
			String winRate) {
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(
				new BetAggregate(new BigDecimal(staked), new BigDecimal(profit), won, 0, 0, 0, 0, null, settled));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.roi()).isEqualByComparingTo(new BigDecimal(roi));
		assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal(winRate));
		assertThat(metrics.settledCount()).isEqualTo(settled);
	}

	// epic-014: os campos novos so precisam ser copiados do agregado bruto pra metrica de negocio
	// - a formula/decisao de negocio (contagem/media) ja e provada no repositorio.
	@Test
	void shouldCopyNewCountsAndAvgOddFromAggregateToMetrics() {
		when(factBetRepository.aggregateOverall(StatisticsFilter.none())).thenReturn(
				new BetAggregate(BigDecimal.valueOf(300), BigDecimal.valueOf(-50), 1, 1, 1, 1, 1,
						BigDecimal.valueOf(1.75), 3));

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
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1))));

		List<SegmentedBetMetrics> result = service.calculateBySport(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		SegmentedBetMetrics segment = result.get(0);
		assertThat(segment.dimensionId()).isEqualTo(sportId);
		assertThat(segment.dimensionName()).isEqualTo("Soccer");
		assertThat(segment.metrics().roi()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
		assertThat(segment.metrics().winRate()).isEqualByComparingTo(BigDecimal.ONE);
	}

	// epic-018: calculateByLeague/calculateByTipster reaproveitam a mesma toSegmentedMetrics de
	// calculateBySport - este teste prova so a fiacao (metodo certo do repositorio chamado), a
	// formula ja esta provada por shouldMapSegmentedAggregatesPreservingDimensionIdentity acima.
	@Test
	void shouldMapByLeagueSegmentFromTheLeagueAggregate() {
		String leagueId = UUID.randomUUID().toString();
		when(factBetRepository.aggregateByLeague(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate(leagueId, "Premier League",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1))));

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
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 0, 0, null, 1))));

		List<SegmentedBetMetrics> result = service.calculateByTipster(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).dimensionId()).isEqualTo(tipsterId);
		assertThat(result.get(0).dimensionName()).isEqualTo("Tipster");
	}

	// epic-014: dimensionId de byBetType e o proprio valor do enum ("PRE"/"LIVE"), nao um uuid -
	// mesmo caminho de mapeamento dos outros segmentos, so a fonte do agregado muda.
	@Test
	void shouldMapByBetTypeSegmentUsingEnumNameAsDimensionId() {
		when(factBetRepository.aggregateByBetType(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate("PRE", "PRE",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 0, 0, 1, 0,
								BigDecimal.valueOf(1.9), 1))));

		List<SegmentedBetMetrics> result = service.calculateByBetType(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		assertThat(result.get(0).dimensionId()).isEqualTo("PRE");
		assertThat(result.get(0).dimensionName()).isEqualTo("PRE");
	}

	// epic-016: mesma regra RN04 de calculateOverall (roi=ZERO se totalStaked=0), reaproveitada
	// pelo helper privado roiOf - shape enxuto (sem wonCount/avgOdd/etc).
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
