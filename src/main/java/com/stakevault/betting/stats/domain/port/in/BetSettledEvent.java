package com.stakevault.betting.stats.domain.port.in;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetStatus;

public record BetSettledEvent(UUID betId, UUID bettingHouseId, String bettingHouseName, UUID sportId,
		String sportName, UUID leagueId, String leagueName, UUID marketId, String marketName, UUID tipsterId,
		String tipsterName, BigDecimal stake, BetStatus status, BigDecimal profit, Instant settledAt) {
}
