package com.stakevault.betting.stats.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ConfidenceIntervalsTest {

	@Test
	void wilsonShouldMatchTheReferenceValueForSevenWinsInTen() {
		ConfidenceInterval interval = ConfidenceIntervals.wilson(7, 10);

		assertThat(interval.lower()).isEqualByComparingTo("0.3968");
		assertThat(interval.upper()).isEqualByComparingTo("0.8922");
	}

	@Test
	void wilsonShouldStayInsideZeroAndOneAtTheExtremes() {
		ConfidenceInterval noWins = ConfidenceIntervals.wilson(0, 5);
		ConfidenceInterval allWins = ConfidenceIntervals.wilson(5, 5);

		assertThat(noWins.lower()).isEqualByComparingTo("0");
		assertThat(noWins.upper()).isEqualByComparingTo("0.4345");
		assertThat(allWins.lower()).isEqualByComparingTo("0.5655");
		assertThat(allWins.upper()).isEqualByComparingTo("1");
	}

	@Test
	void wilsonShouldBeUndefinedWithoutDecidedBets() {
		assertThat(ConfidenceIntervals.wilson(0, 0)).isNull();
	}

	@Test
	void ratioShouldMatchTheDeltaMethodReferenceValue() {
		// stakes 100, 50, 30, 20 e lucros 100, -50, 15, 0: ROI = 65 / 200 = 0,325
		ConfidenceInterval interval = ConfidenceIntervals.ratio(new BigDecimal("65"), new BigDecimal("200"),
				new BigDecimal("12725"), new BigDecimal("13800"), new BigDecimal("7950"), 4);

		assertThat(interval.lower()).isEqualByComparingTo("-0.7494");
		assertThat(interval.upper()).isEqualByComparingTo("1.3994");
	}

	@Test
	void ratioShouldBeUndefinedWithFewerThanTwoBets() {
		assertThat(ConfidenceIntervals.ratio(new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("100"),
				new BigDecimal("100"), new BigDecimal("100"), 1)).isNull();
	}

	@Test
	void ratioShouldBeUndefinedWithoutStake() {
		assertThat(ConfidenceIntervals.ratio(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
				BigDecimal.ZERO, 3)).isNull();
	}

	@Test
	void ratioShouldBeUndefinedWhenEveryBetHasTheSameReturn() {
		// duas apostas de 10 com lucro 5: residuos nulos, sem variancia
		assertThat(ConfidenceIntervals.ratio(new BigDecimal("10"), new BigDecimal("20"), new BigDecimal("50"),
				new BigDecimal("200"), new BigDecimal("100"), 2)).isNull();
	}

	@Test
	void ratioShouldBeUndefinedWhenResidualsCancelOnlyUpToRoundingError() {
		// tres apostas de 99,99 com lucro de 33,33: todas com o mesmo retorno, mas a razao
		// 33,33 / 99,99 = 0,3333... nao e exata em decimal e a soma dos residuos nao zera
		BigDecimal stake = new BigDecimal("99.99");
		BigDecimal profit = new BigDecimal("33.33");
		BigDecimal three = BigDecimal.valueOf(3);
		ConfidenceInterval interval = ConfidenceIntervals.ratio(profit.multiply(three), stake.multiply(three),
				profit.multiply(profit).multiply(three), stake.multiply(stake).multiply(three),
				profit.multiply(stake).multiply(three), 3);

		assertThat(interval).isNull();
	}
}
