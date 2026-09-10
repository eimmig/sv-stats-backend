package com.stakevault.betting.stats.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

class EquityCurveCalculatorTest {

	private static SettledBetPoint point(int day, long profit) {
		return new SettledBetPoint(LocalDate.of(2026, 9, day), BigDecimal.valueOf(profit));
	}

	@Test
	void shouldReturnZeroDrawdownAndNullSharpeForEmptySeries() {
		EquityCurveMetrics metrics = EquityCurveCalculator.calculate(List.of());

		assertThat(metrics.maxDrawdown()).isEqualByComparingTo(BigDecimal.ZERO);
		assertThat(metrics.sharpeRatio()).isNull();
	}

	@Test
	void shouldReturnNullSharpeForSinglePoint() {
		EquityCurveMetrics metrics = EquityCurveCalculator.calculate(List.of(point(1, -50)));

		// pico=0 (antes de qualquer lucro), acumulado=-50 -> drawdown=50.
		assertThat(metrics.maxDrawdown()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(metrics.sharpeRatio()).isNull();
	}

	@Test
	void shouldComputeMaxDrawdownAcrossPeakAndTrough() {
		// acumulado: 100, 50, 80, 0, 20 - pico sempre 100 apos o primeiro ponto - maior
		// drawdown e no ponto onde acumulado cai pra 0 (drawdown = 100).
		List<SettledBetPoint> series = List.of(point(1, 100), point(2, -50), point(3, 30), point(4, -80),
				point(5, 20));

		EquityCurveMetrics metrics = EquityCurveCalculator.calculate(series);

		assertThat(metrics.maxDrawdown()).isEqualByComparingTo(BigDecimal.valueOf(100));
	}

	@Test
	void shouldReturnNullSharpeWhenStandardDeviationIsZero() {
		List<SettledBetPoint> series = List.of(point(1, 10), point(2, 10), point(3, 10));

		EquityCurveMetrics metrics = EquityCurveCalculator.calculate(series);

		assertThat(metrics.sharpeRatio()).isNull();
	}

	@Test
	void shouldComputeSharpeRatioWithSampleStandardDeviation() {
		// media=4.5, desvio-padrao amostral (n-1)=3 (numeros escolhidos para dar raiz exata) ->
		// sharpe = 4.5 / 3 = 1.5.
		List<SettledBetPoint> series = List.of(point(1, 6), point(2, 6), point(3, 6), point(4, 0));

		EquityCurveMetrics metrics = EquityCurveCalculator.calculate(series);

		assertThat(metrics.sharpeRatio()).isEqualByComparingTo(BigDecimal.valueOf(1.5));
	}
}
