package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stakevault.betting.stats.domain.model.BetStatus;

interface FactBetSpringDataRepository extends JpaRepository<FactBetJpaEntity, UUID> {

	// RF11/RN08: predicados opcionais "(:param IS NULL OR ...)" pros UUIDs - StatisticsFilter com
	// todos os campos null equivale a nenhum filtro. O intervalo de data usa limites-sentinela
	// (JpaFactBetRepository substitui from/to null por MIN/MAX) em vez de outro "IS NULL OR" -
	// Postgres nao consegue inferir o tipo de um parametro null usado so dentro de CAST/FUNCTION,
	// achado real durante a implementacao (ver plan_review). FUNCTION('make_date', ...) e
	// Postgres-especifico, unico banco alvo do projeto.
	@Query("""
			SELECT SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND (:bettingHouseId IS NULL OR f.bettingHouseId = :bettingHouseId)
			  AND (:sportId IS NULL OR f.sportId = :sportId)
			  AND (:leagueId IS NULL OR f.leagueId = :leagueId)
			  AND (:marketId IS NULL OR f.marketId = :marketId)
			  AND (:tipsterId IS NULL OR f.tipsterId = :tipsterId)
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :from AND :to
			""")
	AggregateProjection aggregateOverall(@Param("pending") BetStatus pending,
			@Param("bettingHouseId") UUID bettingHouseId, @Param("sportId") UUID sportId,
			@Param("leagueId") UUID leagueId, @Param("marketId") UUID marketId, @Param("tipsterId") UUID tipsterId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT s.id AS dimensionId, s.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimSportJpaEntity s, DimDateJpaEntity d
			WHERE f.sportId = s.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:bettingHouseId IS NULL OR f.bettingHouseId = :bettingHouseId)
			  AND (:sportId IS NULL OR f.sportId = :sportId)
			  AND (:leagueId IS NULL OR f.leagueId = :leagueId)
			  AND (:marketId IS NULL OR f.marketId = :marketId)
			  AND (:tipsterId IS NULL OR f.tipsterId = :tipsterId)
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :from AND :to
			GROUP BY s.id, s.name
			""")
	List<SegmentedAggregateProjection> aggregateBySport(@Param("pending") BetStatus pending,
			@Param("bettingHouseId") UUID bettingHouseId, @Param("sportId") UUID sportId,
			@Param("leagueId") UUID leagueId, @Param("marketId") UUID marketId, @Param("tipsterId") UUID tipsterId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT m.id AS dimensionId, m.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimMarketJpaEntity m, DimDateJpaEntity d
			WHERE f.marketId = m.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:bettingHouseId IS NULL OR f.bettingHouseId = :bettingHouseId)
			  AND (:sportId IS NULL OR f.sportId = :sportId)
			  AND (:leagueId IS NULL OR f.leagueId = :leagueId)
			  AND (:marketId IS NULL OR f.marketId = :marketId)
			  AND (:tipsterId IS NULL OR f.tipsterId = :tipsterId)
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :from AND :to
			GROUP BY m.id, m.name
			""")
	List<SegmentedAggregateProjection> aggregateByMarket(@Param("pending") BetStatus pending,
			@Param("bettingHouseId") UUID bettingHouseId, @Param("sportId") UUID sportId,
			@Param("leagueId") UUID leagueId, @Param("marketId") UUID marketId, @Param("tipsterId") UUID tipsterId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT h.id AS dimensionId, h.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimBettingHouseJpaEntity h, DimDateJpaEntity d
			WHERE f.bettingHouseId = h.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:bettingHouseId IS NULL OR f.bettingHouseId = :bettingHouseId)
			  AND (:sportId IS NULL OR f.sportId = :sportId)
			  AND (:leagueId IS NULL OR f.leagueId = :leagueId)
			  AND (:marketId IS NULL OR f.marketId = :marketId)
			  AND (:tipsterId IS NULL OR f.tipsterId = :tipsterId)
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :from AND :to
			GROUP BY h.id, h.name
			""")
	List<SegmentedAggregateProjection> aggregateByBettingHouse(@Param("pending") BetStatus pending,
			@Param("bettingHouseId") UUID bettingHouseId, @Param("sportId") UUID sportId,
			@Param("leagueId") UUID leagueId, @Param("marketId") UUID marketId, @Param("tipsterId") UUID tipsterId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query("""
			SELECT d.year AS year, d.month AS month, SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND (:bettingHouseId IS NULL OR f.bettingHouseId = :bettingHouseId)
			  AND (:sportId IS NULL OR f.sportId = :sportId)
			  AND (:leagueId IS NULL OR f.leagueId = :leagueId)
			  AND (:marketId IS NULL OR f.marketId = :marketId)
			  AND (:tipsterId IS NULL OR f.tipsterId = :tipsterId)
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :from AND :to
			GROUP BY d.year, d.month
			""")
	List<MonthlyAggregateProjection> aggregateByMonth(@Param("pending") BetStatus pending,
			@Param("bettingHouseId") UUID bettingHouseId, @Param("sportId") UUID sportId,
			@Param("leagueId") UUID leagueId, @Param("marketId") UUID marketId, @Param("tipsterId") UUID tipsterId,
			@Param("from") LocalDate from, @Param("to") LocalDate to);
}
