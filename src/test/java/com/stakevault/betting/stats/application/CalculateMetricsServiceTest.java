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
				new BetAggregate(new BigDecimal(staked), new BigDecimal(profit), won, settled));

		BetMetrics metrics = service.calculateOverall(StatisticsFilter.none());

		assertThat(metrics.roi()).isEqualByComparingTo(new BigDecimal(roi));
		assertThat(metrics.winRate()).isEqualByComparingTo(new BigDecimal(winRate));
		assertThat(metrics.settledCount()).isEqualTo(settled);
	}

	@Test
	void shouldMapSegmentedAggregatesPreservingDimensionIdentity() {
		UUID sportId = UUID.randomUUID();
		when(factBetRepository.aggregateBySport(StatisticsFilter.none())).thenReturn(
				List.of(new SegmentedBetAggregate(sportId, "Soccer",
						new BetAggregate(BigDecimal.valueOf(100), BigDecimal.valueOf(50), 1, 1))));

		List<SegmentedBetMetrics> result = service.calculateBySport(StatisticsFilter.none());

		assertThat(result).hasSize(1);
		SegmentedBetMetrics segment = result.get(0);
		assertThat(segment.dimensionId()).isEqualTo(sportId);
		assertThat(segment.dimensionName()).isEqualTo("Soccer");
		assertThat(segment.metrics().roi()).isEqualByComparingTo(BigDecimal.valueOf(0.5));
		assertThat(segment.metrics().winRate()).isEqualByComparingTo(BigDecimal.ONE);
	}
}
