package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record FactBet(UUID id, UUID dateId, UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId,
		UUID tipsterId, UUID team1Id, UUID team2Id, BigDecimal stake, BigDecimal odd, BigDecimal profit,
		Boolean isWin, BetStatus status, BetType betType, int betCount) {
}
