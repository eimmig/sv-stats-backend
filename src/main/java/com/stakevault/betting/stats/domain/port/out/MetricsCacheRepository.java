package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;

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

	void evict(int year, int month);
}
