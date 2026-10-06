package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

record NewFactBetRow(UUID id, UUID dateId, UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId,
		UUID tipsterId, UUID team1Id, UUID team2Id, BigDecimal stake, BigDecimal odd, BigDecimal profit,
		Boolean isWin, String status, String betType, int betCount) {
}
