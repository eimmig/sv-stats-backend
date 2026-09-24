package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class EquityCurveCalculator {

	private static final int SCALE = 4;

	private EquityCurveCalculator() {
	}

	public static EquityCurveMetrics calculate(List<SettledBetPoint> orderedPoints) {
		return new EquityCurveMetrics(maxDrawdown(orderedPoints), sharpeRatio(orderedPoints));
	}

	public static List<TimelinePoint> timeline(List<SettledBetPoint> orderedPoints) {
		List<TimelinePoint> timeline = new ArrayList<>(orderedPoints.size());
		BigDecimal cumulative = BigDecimal.ZERO;
		for (SettledBetPoint point : orderedPoints) {
			cumulative = cumulative.add(point.profit());
			timeline.add(new TimelinePoint(point.date(), cumulative));
		}
		return timeline;
	}

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
		BigDecimal variance = sumSquaredDiff.divide(BigDecimal.valueOf(n - 1L), MathContext.DECIMAL64);
		if (variance.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}

		BigDecimal stdDev = variance.sqrt(MathContext.DECIMAL64);
		return mean.divide(stdDev, SCALE, RoundingMode.HALF_UP);
	}
}
