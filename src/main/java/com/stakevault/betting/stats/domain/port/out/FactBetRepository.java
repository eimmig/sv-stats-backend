package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.MonthlyBetAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;

public interface FactBetRepository {

	FactBet save(FactBet factBet);

	Optional<FactBet> findById(UUID id);

	// RN06: as 4 agregacoes abaixo ja excluem status=pending. RF11/RN08: filter aplica os
	// predicados opcionais - StatisticsFilter.none() equivale a nenhum filtro.
	BetAggregate aggregateOverall(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateBySport(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByMarket(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByBettingHouse(StatisticsFilter filter);

	List<MonthlyBetAggregate> aggregateByMonth(StatisticsFilter filter);
}
