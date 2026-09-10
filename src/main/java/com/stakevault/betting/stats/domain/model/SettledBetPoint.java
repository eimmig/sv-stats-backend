package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

// Um ponto da serie temporal de apostas liquidadas do recorte de GET /api/v1/statistics/search,
// ordenada por date (ver docs/STATISTICS.md "Drawdown maximo") - date vem de DIM_DATE, que para
// apostas liquidadas reflete a data do JOGO (betDate), preservada por ProcessBetEventService
// desde a correcao do achado real de epic-011 (feat-012.2).
public record SettledBetPoint(LocalDate date, BigDecimal profit) {
}
