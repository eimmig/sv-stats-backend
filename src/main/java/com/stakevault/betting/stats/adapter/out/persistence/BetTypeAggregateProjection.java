package com.stakevault.betting.stats.adapter.out.persistence;

import com.stakevault.betting.stats.domain.model.BetType;

// Achado do plan review de epic-014: getter no tipo real do enum (BetType), conversao pra String
// ("PRE"/"LIVE") feita explicitamente no adapter (JpaFactBetRepository) - nao pedir a esta
// projecao um getDimensionId():String direto de uma expressao JPQL tipada BetType (conversao
// implicita que o resto do codigo nunca faz).
interface BetTypeAggregateProjection extends AggregateProjection {

	BetType getBetType();
}
