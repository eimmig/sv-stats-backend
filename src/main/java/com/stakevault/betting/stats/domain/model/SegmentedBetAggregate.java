package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

// RN09: cada segmento (esporte/mercado/casa de apostas) considera exclusivamente as suas
// proprias apostas - agrupamento feito na propria query (ver FactBetRepository).
public record SegmentedBetAggregate(UUID dimensionId, String dimensionName, BetAggregate aggregate) {
}
