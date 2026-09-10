package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// Resultado de EquityCurveCalculator (ver docs/STATISTICS.md). sharpeRatio pode ser null
// (indeterminado, menos de 2 apostas liquidadas ou desvio-padrao zero - "casos-limite") -
// maxDrawdown nunca e null (serie vazia devolve ZERO).
public record EquityCurveMetrics(BigDecimal maxDrawdown, BigDecimal sharpeRatio) {
}
