package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

// Fato do esquema estrela (docs/DATA-MODEL.md). status/profit/isWin nullable ate a liquidacao -
// insert em BetCreated (status=pending, profit/isWin null), upsert em BetSettled (feat-003).
public record FactBet(UUID id, UUID dateId, UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId,
		UUID tipsterId, BigDecimal stake, BigDecimal profit, Boolean isWin, BetStatus status, int betCount) {
}
