package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record EquityCurveMetrics(BigDecimal maxDrawdown, BigDecimal sharpeRatio) {
}
