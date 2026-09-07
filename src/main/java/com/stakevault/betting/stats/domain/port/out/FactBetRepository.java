package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.MonthlyBetAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;

public interface FactBetRepository {

	FactBet save(FactBet factBet);

	Optional<FactBet> findById(UUID id);

	// RN06: as 4 agregacoes abaixo ja excluem status=pending.
	BetAggregate aggregateOverall();

	List<SegmentedBetAggregate> aggregateBySport();

	List<SegmentedBetAggregate> aggregateByMarket();

	List<SegmentedBetAggregate> aggregateByBettingHouse();

	List<MonthlyBetAggregate> aggregateByMonth();
}
