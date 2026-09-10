package com.stakevault.betting.stats.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.EquityCurveCalculator;
import com.stakevault.betting.stats.domain.model.EquityCurveMetrics;
import com.stakevault.betting.stats.domain.model.SearchAggregate;
import com.stakevault.betting.stats.domain.model.SettledBetPoint;
import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchResult;
import com.stakevault.betting.stats.domain.model.StatisticsSearchSummary;
import com.stakevault.betting.stats.domain.model.TimelinePoint;
import com.stakevault.betting.stats.domain.port.in.SearchStatisticsUseCase;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;

// GET /api/v1/statistics/search (epic-011) - diferente de GetStatisticsDashboardService
// (feat-006), sempre calcula direto contra FACT_BET, sem cache-aside (espaco de combinacoes
// esporte x liga x time x mercado x tipster x periodo grande demais pro padrao de chave fixa por
// tenant de feat-005, ver docs/API-CONTRACTS.md).
@Service
public class SearchStatisticsService implements SearchStatisticsUseCase {

	private static final int SCALE = 4;

	private final FactBetRepository factBetRepository;

	public SearchStatisticsService(FactBetRepository factBetRepository) {
		this.factBetRepository = factBetRepository;
	}

	@Override
	public StatisticsSearchResult search(StatisticsSearchFilter filter) {
		SearchAggregate aggregate = factBetRepository.aggregateForSearch(filter);
		List<SettledBetPoint> orderedPoints = factBetRepository.findOrderedSettledProfits(filter);
		EquityCurveMetrics equityCurve = EquityCurveCalculator.calculate(orderedPoints);
		List<TimelinePoint> timeline = EquityCurveCalculator.timeline(orderedPoints);

		StatisticsSearchSummary summary = new StatisticsSearchSummary(aggregate.settledCount(),
				aggregate.totalStaked(), aggregate.netProfit(), roi(aggregate), winRate(aggregate),
				aggregate.avgOdd(), equityCurve.maxDrawdown(), equityCurve.sharpeRatio());

		return new StatisticsSearchResult(filter, summary, timeline);
	}

	// RN04, mesma formula/decisao de zero-division de CalculateMetricsService (feat-004) - nenhuma
	// aposta liquidada no recorte retorna ZERO, nao excecao/null.
	private static BigDecimal roi(SearchAggregate aggregate) {
		return aggregate.totalStaked().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
				: aggregate.netProfit().divide(aggregate.totalStaked(), SCALE, RoundingMode.HALF_UP);
	}

	private static BigDecimal winRate(SearchAggregate aggregate) {
		return aggregate.settledCount() == 0 ? BigDecimal.ZERO
				: BigDecimal.valueOf(aggregate.wonCount())
						.divide(BigDecimal.valueOf(aggregate.settledCount()), SCALE, RoundingMode.HALF_UP);
	}
}
