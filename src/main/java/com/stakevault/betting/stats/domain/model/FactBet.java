package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

// Fato do esquema estrela (docs/DATA-MODEL.md). status/profit/isWin nullable ate a liquidacao -
// insert em BetCreated (status=pending, profit/isWin null), upsert em BetSettled. team1Id/
// team2Id/odd nullable porque linhas de antes daqueles campos existirem nao tem como ser
// retroalimentadas (o evento original ja foi consumido e descartado). betType nullable -
// gravado so no insert de BetCreated, preservado (nunca recalculado) no upsert de BetSettled,
// porque aquele payload nao carrega betType (ver ProcessBetEventService).
public record FactBet(UUID id, UUID dateId, UUID bettingHouseId, UUID sportId, UUID leagueId, UUID marketId,
		UUID tipsterId, UUID team1Id, UUID team2Id, BigDecimal stake, BigDecimal odd, BigDecimal profit,
		Boolean isWin, BetStatus status, BetType betType, int betCount) {
}
