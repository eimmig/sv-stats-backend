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
	void ratioShouldMatchTheDeltaMethodReferenceValueWithTheStudentQuantile() {
		// stakes 100, 50, 30, 20 e lucros 100, -50, 15, 0: ROI = 65 / 200 = 0,325, erro padrao
		// 0,5482 e t(0,975; 3) = 3,1824 (com z = 1,96 o intervalo seria -0,7494 a 1,3994)
		ConfidenceInterval interval = ConfidenceIntervals.ratio(new BigDecimal("65"), new BigDecimal("200"),
				new BigDecimal("12725"), new BigDecimal("13800"), new BigDecimal("7950"), 4);

		assertThat(interval.lower()).isEqualByComparingTo("-1.4195");
		assertThat(interval.upper()).isEqualByComparingTo("2.0695");
	}

	@Test
	void studentQuantileShouldMatchTheTableAndApproachTheNormalOne() {
		assertThat(StudentT.quantile975(1)).isEqualByComparingTo("12.7062");
		assertThat(StudentT.quantile975(5)).isEqualByComparingTo("2.5706");
		assertThat(StudentT.quantile975(30)).isEqualByComparingTo("2.0423");
		assertThat(StudentT.quantile975(40).doubleValue()).isCloseTo(2.0211, org.assertj.core.data.Offset.offset(1e-4));
		assertThat(StudentT.quantile975(120).doubleValue()).isCloseTo(1.9799, org.assertj.core.data.Offset.offset(1e-4));
		assertThat(StudentT.quantile975(100_000).doubleValue()).isCloseTo(1.96, org.assertj.core.data.Offset.offset(1e-4));
	}

	@Test
	void studentQuantileShouldRejectZeroDegreesOfFreedom() {
		org.assertj.core.api.Assertions.assertThatThrownBy(() -> StudentT.quantile975(0))
				.isInstanceOf(IllegalArgumentException.class);
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
