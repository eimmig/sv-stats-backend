package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// Soma bruta sobre FACT_BET (RN06: ja filtrada para status <> pending) - sem ROI/taxa de acerto
// ainda calculados, ver CalculateMetricsUseCase para a formula (RN04/RN09). lostCount/voidCount
// somam wonCount para settledCount; preCount/liveCount contam so apostas com betType classificado
// (epic-014) - podem somar menos que settledCount. avgOdd nullable (sem apostas com odd no
// recorte), nunca coalescido pra ZERO, mesmo tratamento de SearchAggregate.avgOdd.
public record BetAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long lostCount,
		long voidCount, long preCount, long liveCount, BigDecimal avgOdd, long settledCount) {
}
