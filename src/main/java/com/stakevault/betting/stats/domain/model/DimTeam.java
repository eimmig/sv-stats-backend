package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

// Diferente das demais dimensoes nominais (id = mesmo uuid do catalogo em bets-service),
// team1/team2 sao texto livre por aposta (sem catalogo em bets-service) - id e gerado por este
// servico na primeira vez que o nome aparece. Chave natural composta (name, sportId) - decisao
// do usuario, 2026-09-10 (feat-013): o mesmo nome de time pode existir em esportes diferentes,
// entao name sozinho nao basta; sportId tambem viabiliza escopar GET /api/v1/statistics/teams
// por esporte (autocomplete da tela "Buscar Estatisticas" refiltra ao trocar de esporte).
public record DimTeam(UUID id, String name, UUID sportId) {
}
