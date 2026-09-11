package com.stakevault.betting.stats.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.DailyBetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;

@Service
public class CalculateMetricsService implements CalculateMetricsUseCase {

	private static final int SCALE = 4;

	private final FactBetRepository factBetRepository;

	public CalculateMetricsService(FactBetRepository factBetRepository) {
		this.factBetRepository = factBetRepository;
	}

	@Override
	public BetMetrics calculateOverall(StatisticsFilter filter) {
		return toMetrics(factBetRepository.aggregateOverall(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateBySport(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateBySport(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateByMarket(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByMarket(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateByBettingHouse(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByBettingHouse(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateByBetType(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByBetType(filter));
	}

	@Override
	public List<MonthlyBetMetrics> calculateMonthly(StatisticsFilter filter) {
		return factBetRepository.aggregateByMonth(filter)
				.stream()
				.map(monthly -> new MonthlyBetMetrics(monthly.year(), monthly.month(), toMetrics(monthly.aggregate())))
				.toList();
	}

	@Override
	public List<DailyBetMetrics> calculateDaily(StatisticsFilter filter) {
		return factBetRepository.aggregateByDay(filter)
				.stream()
				.map(daily -> new DailyBetMetrics(daily.date(), daily.totalStaked(), daily.netProfit(),
						roiOf(daily.netProfit(), daily.totalStaked()), daily.betCount()))
				.toList();
	}

	private static List<SegmentedBetMetrics> toSegmentedMetrics(List<SegmentedBetAggregate> segments) {
		return segments.stream()
				.map(segment -> new SegmentedBetMetrics(segment.dimensionId(), segment.dimensionName(),
						toMetrics(segment.aggregate())))
				.toList();
	}

	// RN04: roi = lucro liquido acumulado / valor total investido. Taxa de acerto = vitorias /
	// liquidadas. Nenhuma aposta liquidada (total investido=0 ou liquidadas=0) retorna ZERO, nao
	// excecao/null - RN04 nao define esse caso, decisao registrada no plan_review de feat-004.
	private static BetMetrics toMetrics(BetAggregate aggregate) {
		BigDecimal roi = roiOf(aggregate.netProfit(), aggregate.totalStaked());
		BigDecimal winRate = aggregate.settledCount() == 0 ? BigDecimal.ZERO
				: BigDecimal.valueOf(aggregate.wonCount())
						.divide(BigDecimal.valueOf(aggregate.settledCount()), SCALE, RoundingMode.HALF_UP);
		return new BetMetrics(aggregate.totalStaked(), aggregate.netProfit(), roi, winRate, aggregate.settledCount(),
				aggregate.wonCount(), aggregate.lostCount(), aggregate.voidCount(), aggregate.preCount(),
				aggregate.liveCount(), aggregate.avgOdd());
	}

	// epic-016: mesma regra de roi=ZERO-se-totalStaked=0 (RN04) reaproveitada por toMetrics() e
	// calculateDaily() - extraido pra evitar duplicar a formula.
	private static BigDecimal roiOf(BigDecimal netProfit, BigDecimal totalStaked) {
		return totalStaked.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
				: netProfit.divide(totalStaked, SCALE, RoundingMode.HALF_UP);
	}
}
