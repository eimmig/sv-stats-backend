package com.stakevault.betting.stats.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.ConfidenceInterval;
import com.stakevault.betting.stats.domain.model.ConfidenceIntervals;
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
	public List<SegmentedBetMetrics> calculateByLeague(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByLeague(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateByTipster(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByTipster(filter));
	}

	@Override
	public List<SegmentedBetMetrics> calculateByTeam(StatisticsFilter filter) {
		return toSegmentedMetrics(factBetRepository.aggregateByTeam(filter));
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

	private static BetMetrics toMetrics(BetAggregate aggregate) {
		BigDecimal roi = roiOf(aggregate.netProfit(), aggregate.totalStaked());
		long decided = aggregate.wonCount() + aggregate.lostCount();
		BigDecimal winRate = decided == 0 ? BigDecimal.ZERO
				: BigDecimal.valueOf(aggregate.wonCount())
						.divide(BigDecimal.valueOf(decided), SCALE, RoundingMode.HALF_UP);
		BigDecimal impliedProbability = aggregate.impliedProbability() == null ? null
				: aggregate.impliedProbability().setScale(SCALE, RoundingMode.HALF_UP);
		// a vantagem so compara populacoes iguais: indefinida se alguma decidida nao tem odd
		BigDecimal edge = impliedProbability == null || aggregate.decidedWithOddCount() != decided ? null
				: winRate.subtract(impliedProbability);
		ConfidenceInterval winRateInterval = ConfidenceIntervals.wilson(aggregate.wonCount(), decided);
		ConfidenceInterval roiInterval = ConfidenceIntervals.ratio(aggregate.netProfit(), aggregate.totalStaked(),
				aggregate.profitSquaredSum(), aggregate.stakeSquaredSum(), aggregate.profitStakeSum(),
				aggregate.settledCount());
		return new BetMetrics(aggregate.totalStaked(), aggregate.netProfit(), roi, winRate, aggregate.settledCount(),
				aggregate.wonCount(), aggregate.lostCount(), aggregate.voidCount(), aggregate.preCount(),
				aggregate.liveCount(), aggregate.avgOdd(), impliedProbability, edge, lower(winRateInterval),
				upper(winRateInterval), lower(roiInterval), upper(roiInterval));
	}

	private static BigDecimal lower(ConfidenceInterval interval) {
		return interval == null ? null : interval.lower();
	}

	private static BigDecimal upper(ConfidenceInterval interval) {
		return interval == null ? null : interval.upper();
	}

	private static BigDecimal roiOf(BigDecimal netProfit, BigDecimal totalStaked) {
		return totalStaked.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
				: netProfit.divide(totalStaked, SCALE, RoundingMode.HALF_UP);
	}
}
