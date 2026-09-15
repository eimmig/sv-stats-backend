package com.stakevault.betting.stats.domain.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetType;

public record BetCreatedEvent(UUID betId, UUID bettingHouseId, String bettingHouseName, UUID sportId,
		String sportName, UUID leagueId, String leagueName, UUID marketId, String marketName, UUID tipsterId,
		String tipsterName, String team1, String team2, BigDecimal stake, BigDecimal odd, BetType betType,
		Instant betDate) {
}
