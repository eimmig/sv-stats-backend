package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

// id e o mesmo uuid do catalogo em bets-service (bettingHouseId), nao gerado por este servico -
// name denormalizado do payload do evento (ver docs/API-CONTRACTS.md).
public record DimBettingHouse(UUID id, String name) {
}
