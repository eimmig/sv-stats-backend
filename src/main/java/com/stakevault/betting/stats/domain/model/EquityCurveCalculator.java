package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.List;

// Calculo puro, sem I/O (domain - ver docs/STATISTICS.md "Drawdown maximo"/"Indice de Sharpe
// simplificado") - recebe a serie de apostas liquidadas ja ordenada por data (mesma ordem devolvida
// por FactBetRepository.findOrderedSettledProfits), sem acessar repositorio nem framework.
public final class EquityCurveCalculator {

	private static final int SCALE = 4;

	private EquityCurveCalculator() {
	}

	public static EquityCurveMetrics calculate(List<SettledBetPoint> orderedPoints) {
		return new EquityCurveMetrics(maxDrawdown(orderedPoints), sharpeRatio(orderedPoints));
	}

	// Maior queda pico-a-vale no lucro acumulado, em valor absoluto (mesma unidade de
	// stake/profit) - serie vazia devolve ZERO, nao excecao.
	private static BigDecimal maxDrawdown(List<SettledBetPoint> orderedPoints) {
		BigDecimal cumulative = BigDecimal.ZERO;
		BigDecimal peak = BigDecimal.ZERO;
		BigDecimal maxDrawdown = BigDecimal.ZERO;
		for (SettledBetPoint point : orderedPoints) {
			cumulative = cumulative.add(point.profit());
			peak = peak.max(cumulative);
			maxDrawdown = maxDrawdown.max(peak.subtract(cumulative));
		}
		return maxDrawdown;
	}

	// Media/desvio-padrao AMOSTRAL (n-1, nao populacional - o recorte e sempre uma amostra) do
	// profit por aposta liquidada. null (indeterminado) com menos de 2 pontos ou desvio-padrao
	// zero (todas as apostas com o mesmo profit) - nunca divisao por zero.
	private static BigDecimal sharpeRatio(List<SettledBetPoint> orderedPoints) {
		int n = orderedPoints.size();
		if (n < 2) {
			return null;
		}

		BigDecimal sum = BigDecimal.ZERO;
		for (SettledBetPoint point : orderedPoints) {
			sum = sum.add(point.profit());
		}
		BigDecimal mean = sum.divide(BigDecimal.valueOf(n), MathContext.DECIMAL64);

		BigDecimal sumSquaredDiff = BigDecimal.ZERO;
		for (SettledBetPoint point : orderedPoints) {
			BigDecimal diff = point.profit().subtract(mean);
			sumSquaredDiff = sumSquaredDiff.add(diff.multiply(diff));
		}
		BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(n - 1), MathContext.DECIMAL64);
		if (variance.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}

		BigDecimal stdDev = variance.sqrt(MathContext.DECIMAL64);
		return mean.divide(stdDev, SCALE, RoundingMode.HALF_UP);
	}
}
