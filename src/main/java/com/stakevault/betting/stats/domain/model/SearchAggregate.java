package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

// Agregado bruto especifico de GET /api/v1/statistics/search - separado de BetAggregate (usado
// pelo dashboard consolidado) para nao colidir com a extensao de avgOdd do bundle consolidado
// (ver docs/API-CONTRACTS.md) - cada endpoint tem seu proprio agregado, sem acoplar os dois.
public record SearchAggregate(BigDecimal totalStaked, BigDecimal netProfit, long wonCount, long settledCount,
		BigDecimal avgOdd) {
}
