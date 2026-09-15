package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// RN04: roi = lucro liquido acumulado / valor total investido. Taxa de acerto = vitorias /
// liquidadas. Ambas zero (nao excecao/null) quando o denominador e zero - ver
// CalculateMetricsUseCase. wonCount/lostCount/voidCount/preCount/liveCount/avgOdd (epic-014) -
// avgOdd nullable (nunca coalescido pra ZERO, mesmo tratamento de SearchAggregate.avgOdd).
public record BetMetrics(BigDecimal totalStaked, BigDecimal netProfit, BigDecimal roi, BigDecimal winRate,
		long settledCount, long wonCount, long lostCount, long voidCount, long preCount, long liveCount,
		BigDecimal avgOdd) {
}
