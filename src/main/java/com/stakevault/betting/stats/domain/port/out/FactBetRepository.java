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

	// RN06: as 4 agregacoes abaixo ja excluem status=pending. RF11/RN08: filter aplica os
	// predicados opcionais - StatisticsFilter.none() equivale a nenhum filtro.
	BetAggregate aggregateOverall(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateBySport(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByMarket(StatisticsFilter filter);

	List<SegmentedBetAggregate> aggregateByBettingHouse(StatisticsFilter filter);

	List<MonthlyBetAggregate> aggregateByMonth(StatisticsFilter filter);

	// 6o segmento (epic-014) - so 2 buckets fixos (PRE/LIVE), apostas sem betType classificado
	// ficam de fora dos dois.
	List<SegmentedBetAggregate> aggregateByBetType(StatisticsFilter filter);

	// epic-016 (GET /api/v1/statistics/daily): array esparso ordenado por data ascendente - so
	// dias com pelo menos 1 aposta liquidada geram linha.
	List<DailyBetAggregate> aggregateByDay(StatisticsFilter filter);

	// epic-011 (RF09 estendido): agregado + serie ordenada especificos de GET
	// /api/v1/statistics/search - separados dos 5 metodos acima (dashboard consolidado, feat-006)
	// porque o filtro exige sportId/leagueId e adiciona teamId (team1Id OR team2Id).
	SearchAggregate aggregateForSearch(StatisticsSearchFilter filter);

	// Ordenada por date crescente - base do calculo de drawdown/Sharpe (domain, feat-012.4) e do
	// campo "timeline" da resposta HTTP (feat-012.5).
	List<SettledBetPoint> findOrderedSettledProfits(StatisticsSearchFilter filter);
}
