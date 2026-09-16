package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

// Origem do id e assimetrica (desde bets-service feat-017 introduzir o catalogo TEAM,
// docs/DECISIONS-LOG.md 2026-09-15/16): times vistos pela primeira vez neste servico depois
// dessa mudanca ganham o mesmo uuid do catalogo real de bets-service (igual as demais dimensoes
// nominais); times ja existentes antes (resolvidos so por nome, epic-011) mantem o id gerado
// localmente para sempre, sem backfill (mesmo precedente de V20260910130000) - ver
// DimensionResolver.resolveTeam. Chave natural composta (name, sportId) continua sendo o que
// garante que o mesmo time nunca duplica: o mesmo nome de time pode existir em esportes
// diferentes, entao name sozinho nao basta; sportId tambem viabiliza escopar
// GET /api/v1/statistics/teams por esporte (autocomplete da tela "Buscar Estatisticas" refiltra
// ao trocar de esporte).
public record DimTeam(UUID id, String name, UUID sportId) {
}
