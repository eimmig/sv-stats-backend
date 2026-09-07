package com.stakevault.betting.stats.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
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
	public BetMetrics calculateOverall() {
		return toMetrics(factBetRepository.aggregateOverall());
	}

	@Override
	public List<SegmentedBetMetrics> calculateBySport() {
		return toSegmentedMetrics(factBetRepository.aggregateBySport());
	}

	@Override
	public List<SegmentedBetMetrics> calculateByMarket() {
		return toSegmentedMetrics(factBetRepository.aggregateByMarket());
	}

	@Override
	public List<SegmentedBetMetrics> calculateByBettingHouse() {
		return toSegmentedMetrics(factBetRepository.aggregateByBettingHouse());
	}

	@Override
	public List<MonthlyBetMetrics> calculateMonthly() {
		return factBetRepository.aggregateByMonth()
				.stream()
				.map(monthly -> new MonthlyBetMetrics(monthly.year(), monthly.month(), toMetrics(monthly.aggregate())))
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
		BigDecimal roi = aggregate.totalStaked().compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
				: aggregate.netProfit().divide(aggregate.totalStaked(), SCALE, RoundingMode.HALF_UP);
		BigDecimal winRate = aggregate.settledCount() == 0 ? BigDecimal.ZERO
				: BigDecimal.valueOf(aggregate.wonCount())
						.divide(BigDecimal.valueOf(aggregate.settledCount()), SCALE, RoundingMode.HALF_UP);
		return new BetMetrics(aggregate.totalStaked(), aggregate.netProfit(), roi, winRate, aggregate.settledCount());
	}
}
