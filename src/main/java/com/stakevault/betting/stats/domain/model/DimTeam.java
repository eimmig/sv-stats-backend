package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

// Diferente das demais dimensoes nominais (id = mesmo uuid do catalogo em bets-service),
// team1/team2 sao texto livre por aposta (sem catalogo em bets-service) - id e gerado por este
// servico na primeira vez que o nome aparece, resolvido por chave natural (name), mesmo padrao
// de DimDate.
public record DimTeam(UUID id, String name) {
}
