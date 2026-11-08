package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * Intervalos de confianca de 95% dos indicadores do bundle de estatisticas (feat-033, ver
 * docs/estatisticas.md). Devolvem null quando o intervalo nao esta definido, nunca um intervalo
 * degenerado.
 */
public final class ConfidenceIntervals {

	private static final int SCALE = 4;
	private static final MathContext MC = MathContext.DECIMAL64;
	private static final BigDecimal Z = new BigDecimal("1.96");
	private static final BigDecimal Z_SQUARED = Z.multiply(Z);
	private static final BigDecimal TWO = BigDecimal.valueOf(2);
	private static final BigDecimal FOUR = BigDecimal.valueOf(4);

	private ConfidenceIntervals() {
	}

	/**
	 * Intervalo de Wilson para a proporcao {@code successes / trials} (taxa de acerto, com
	 * trials = ganhas + perdidas). Nulo quando trials = 0.
	 */
	public static ConfidenceInterval wilson(long successes, long trials) {
		if (trials <= 0) {
			return null;
		}
		BigDecimal n = BigDecimal.valueOf(trials);
		BigDecimal p = BigDecimal.valueOf(successes).divide(n, MC);
		BigDecimal denominator = BigDecimal.ONE.add(Z_SQUARED.divide(n, MC));
		BigDecimal center = p.add(Z_SQUARED.divide(TWO.multiply(n), MC)).divide(denominator, MC);
		BigDecimal spread = p.multiply(BigDecimal.ONE.subtract(p)).divide(n, MC)
				.add(Z_SQUARED.divide(FOUR.multiply(n).multiply(n), MC));
		BigDecimal halfWidth = Z.multiply(spread.sqrt(MC)).divide(denominator, MC);
		return new ConfidenceInterval(clampToUnit(center.subtract(halfWidth)), clampToUnit(center.add(halfWidth)));
	}

	/**
	 * Intervalo pelo metodo delta para o estimador de razao R = sum(l) / sum(s) (ROI, com n =
	 * apostas liquidadas). Usa os residuos d_i = l_i - R * s_i, cuja soma dos quadrados sai dos
	 * somatorios sum(l^2), sum(s^2) e sum(l*s) sem reler as apostas. Nulo quando n < 2, sem valor
	 * apostado ou sem variancia.
	 */
	public static ConfidenceInterval ratio(BigDecimal profitSum, BigDecimal stakeSum, BigDecimal profitSquaredSum,
			BigDecimal stakeSquaredSum, BigDecimal profitStakeSum, long count) {
		if (count < 2 || stakeSum.signum() == 0) {
			return null;
		}
		BigDecimal n = BigDecimal.valueOf(count);
		BigDecimal ratio = profitSum.divide(stakeSum, MC);
		BigDecimal residualSquares = profitSquaredSum.subtract(TWO.multiply(ratio).multiply(profitStakeSum))
				.add(ratio.multiply(ratio).multiply(stakeSquaredSum));
		if (residualSquares.signum() <= 0) {
			return null;
		}
		BigDecimal variance = residualSquares.divide(n.subtract(BigDecimal.ONE), MC);
		BigDecimal meanStake = stakeSum.divide(n, MC);
		BigDecimal standardError = variance.divide(n, MC).sqrt(MC).divide(meanStake, MC);
		BigDecimal halfWidth = Z.multiply(standardError);
		return new ConfidenceInterval(round(ratio.subtract(halfWidth)), round(ratio.add(halfWidth)));
	}

	private static BigDecimal clampToUnit(BigDecimal value) {
		return round(value.max(BigDecimal.ZERO).min(BigDecimal.ONE));
	}

	private static BigDecimal round(BigDecimal value) {
		return value.setScale(SCALE, RoundingMode.HALF_UP);
	}
}
