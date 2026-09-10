package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// Agregado bruto especifico de GET /api/v1/statistics/search - separado de BetAggregate (usado
// pelo dashboard consolidado, feat-006) para nao colidir com a extensao de avgOdd planejada
// separadamente para o bundle consolidado (epic-014, ver docs/API-CONTRACTS.md) - cada endpoint
// tem seu proprio agregado, sem acoplar os dois.
public record SearchAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long settledCount,
		BigDecimal avgOdd) {
}
