package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.DailyBetAggregate;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.MonthlyBetAggregate;
import com.stakevault.betting.stats.domain.model.SearchAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.SettledBetPoint;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;

public interface FactBetRepository {

	FactBet save(FactBet factBet);

	Optional<FactBet> findById(UUID id);

	BetAggregate aggregateOverall(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateBySport(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByMarket(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByBettingHouse(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByLeague(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByTipster(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByTeam(StatisticsFilter filter);

	List<MonthlyBetAggregate> aggregateByMonth(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByBetType(StatisticsFilter filter);

	List<DailyBetAggregate> aggregateByDay(StatisticsFilter filter);

	SearchAggregate aggregateForSearch(StatisticsSearchFilter filter);

	List<SettledBetPoint> findOrderedSettledProfits(StatisticsSearchFilter filter);
}
