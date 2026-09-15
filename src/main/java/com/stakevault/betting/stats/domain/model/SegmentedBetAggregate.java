package com.stakevault.betting.stats.domain.model;

// RN09: cada segmento (esporte/mercado/casa de apostas/tipo de aposta) considera exclusivamente
// as suas proprias apostas - agrupamento feito na propria query (ver FactBetRepository).
// dimensionId e String (nao UUID) desde epic-014 - byBetType usa o proprio nome do enum
// ("PRE"/"LIVE") como identificador, unico segmento sem uuid de catalogo por tras; os demais
// segmentos convertem UUID.toString() na fronteira do adapter (ver JpaFactBetRepository).
public record SegmentedBetAggregate(String dimensionId, String dimensionName, BetAggregate aggregate) {
}
