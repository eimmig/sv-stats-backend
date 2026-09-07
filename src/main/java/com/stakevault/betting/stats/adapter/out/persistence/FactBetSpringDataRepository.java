package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stakevault.betting.stats.domain.model.BetStatus;

interface FactBetSpringDataRepository extends JpaRepository<FactBetJpaEntity, UUID> {

	@Query("""
			SELECT SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f
			WHERE f.status <> :pending
			""")
	AggregateProjection aggregateOverall(@Param("pending") BetStatus pending);

	@Query("""
			SELECT s.id AS dimensionId, s.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimSportJpaEntity s
			WHERE f.sportId = s.id AND f.status <> :pending
			GROUP BY s.id, s.name
			""")
	List<SegmentedAggregateProjection> aggregateBySport(@Param("pending") BetStatus pending);

	@Query("""
			SELECT m.id AS dimensionId, m.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimMarketJpaEntity m
			WHERE f.marketId = m.id AND f.status <> :pending
			GROUP BY m.id, m.name
			""")
	List<SegmentedAggregateProjection> aggregateByMarket(@Param("pending") BetStatus pending);

	@Query("""
			SELECT h.id AS dimensionId, h.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimBettingHouseJpaEntity h
			WHERE f.bettingHouseId = h.id AND f.status <> :pending
			GROUP BY h.id, h.name
			""")
	List<SegmentedAggregateProjection> aggregateByBettingHouse(@Param("pending") BetStatus pending);

	@Query("""
			SELECT d.year AS year, d.month AS month, SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			GROUP BY d.year, d.month
			""")
	List<MonthlyAggregateProjection> aggregateByMonth(@Param("pending") BetStatus pending);
}
