package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;

public interface MetricsCacheRepository {

	Optional<BetMetrics> findOverall();

	void saveOverall(BetMetrics metrics);

	Optional<List<SegmentedBetMetrics>> findBySport();

	void saveBySport(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByMarket();

	void saveByMarket(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByBettingHouse();

	void saveByBettingHouse(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByLeague();

	void saveByLeague(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByTipster();

	void saveByTipster(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByTeam();

	void saveByTeam(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByBetType();

	void saveByBetType(List<SegmentedBetMetrics> metrics);

	Optional<BetMetrics> findMonthly(int year, int month);

	void saveMonthly(int year, int month, BetMetrics metrics);

	Optional<List<MonthlyBetMetrics>> findMonthlySeries();

	void saveMonthlySeries(List<MonthlyBetMetrics> series);

	Optional<StatisticsDashboard> findDashboard(StatisticsFilter filter);

	void saveDashboard(StatisticsFilter filter, StatisticsDashboard dashboard);

	/** Invalida a visao geral, o mes e todos os dashboards filtrados da organizacao corrente. */
	void evict(int year, int month);
}
