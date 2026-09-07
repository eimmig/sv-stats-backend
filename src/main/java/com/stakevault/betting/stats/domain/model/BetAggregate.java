package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// Soma bruta sobre FACT_BET (RN06: ja filtrada para status <> pending) - sem ROI/taxa de acerto
// ainda calculados, ver CalculateMetricsUseCase para a formula (RN04/RN09).
public record BetAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long settledCount) {
}
