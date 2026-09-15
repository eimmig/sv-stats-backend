package com.stakevault.betting.stats.application;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.CalculateMetricsUseCase;
import com.stakevault.betting.stats.domain.port.in.GetDashboardMetricsUseCase;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

@Service
public class GetDashboardMetricsService implements GetDashboardMetricsUseCase {

	private static final BetMetrics ZERO_METRICS = new BetMetrics(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
			BigDecimal.ZERO, 0, 0, 0, 0, 0, 0, null);

	private final CalculateMetricsUseCase calculateMetrics;
	private final MetricsCacheRepository cache;

	public GetDashboardMetricsService(CalculateMetricsUseCase calculateMetrics, MetricsCacheRepository cache) {
		this.calculateMetrics = calculateMetrics;
		this.cache = cache;
	}

	@Override
	public BetMetrics getOverall() {
		return cache.findOverall().orElseGet(() -> {
			BetMetrics metrics = calculateMetrics.calculateOverall(StatisticsFilter.none());
			cache.saveOverall(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getBySport() {
		return cache.findBySport().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateBySport(StatisticsFilter.none());
			cache.saveBySport(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getByMarket() {
		return cache.findByMarket().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateByMarket(StatisticsFilter.none());
			cache.saveByMarket(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getByBettingHouse() {
		return cache.findByBettingHouse().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateByBettingHouse(StatisticsFilter.none());
			cache.saveByBettingHouse(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getByLeague() {
		return cache.findByLeague().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateByLeague(StatisticsFilter.none());
			cache.saveByLeague(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getByTipster() {
		return cache.findByTipster().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateByTipster(StatisticsFilter.none());
			cache.saveByTipster(metrics);
			return metrics;
		});
	}

	@Override
	public List<SegmentedBetMetrics> getByBetType() {
		return cache.findByBetType().orElseGet(() -> {
			List<SegmentedBetMetrics> metrics = calculateMetrics.calculateByBetType(StatisticsFilter.none());
			cache.saveByBetType(metrics);
			return metrics;
		});
	}

	@Override
	public BetMetrics getMonthly(int year, int month) {
		return cache.findMonthly(year, month).orElseGet(() -> {
			// Um miss recalcula e cacheia a serie inteira de uma vez (poucos meses com dados,
			// ver plan_review) - aproveita pra aquecer o cache dos outros meses tambem.
			List<MonthlyBetMetrics> series = calculateMetrics.calculateMonthly(StatisticsFilter.none());
			series.forEach(monthly -> cache.saveMonthly(monthly.year(), monthly.month(), monthly.metrics()));
			BetMetrics requested = series.stream()
					.filter(monthly -> monthly.year() == year && monthly.month() == month)
					.map(MonthlyBetMetrics::metrics)
					.findFirst()
					.orElse(ZERO_METRICS);
			// Mes sem nenhuma aposta liquidada nunca aparece em aggregateByMonth() (GROUP BY so
			// retorna grupo existente) - sem isso, toda consulta a um mes vazio recalcularia a
			// serie inteira de novo, achado real do self-review (Delivery/Test Suite Auditor).
			if (series.stream().noneMatch(monthly -> monthly.year() == year && monthly.month() == month)) {
				cache.saveMonthly(year, month, ZERO_METRICS);
			}
			return requested;
		});
	}
}
