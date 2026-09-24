package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;

interface FactBetSpringDataRepository extends JpaRepository<FactBetJpaEntity, UUID> {

	@Query("""
			SELECT SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			""")
	AggregateProjection aggregateOverall(@Param("pending") BetStatus pending, @Param("lost") BetStatus lost,
			@Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre, @Param("live") BetType live,
			@Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT s.id AS dimensionId, s.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimSportJpaEntity s, DimDateJpaEntity d
			WHERE f.sportId = s.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY s.id, s.name
			""")
	List<SegmentedAggregateProjection> aggregateBySport(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT m.id AS dimensionId, m.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimMarketJpaEntity m, DimDateJpaEntity d
			WHERE f.marketId = m.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY m.id, m.name
			""")
	List<SegmentedAggregateProjection> aggregateByMarket(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT h.id AS dimensionId, h.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimBettingHouseJpaEntity h, DimDateJpaEntity d
			WHERE f.bettingHouseId = h.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY h.id, h.name
			""")
	List<SegmentedAggregateProjection> aggregateByBettingHouse(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT l.id AS dimensionId, l.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimLeagueJpaEntity l, DimDateJpaEntity d
			WHERE f.leagueId = l.id AND f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY l.id, l.name
			""")
	List<SegmentedAggregateProjection> aggregateByLeague(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT t.id AS dimensionId, t.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimTipsterJpaEntity t, DimDateJpaEntity d
			WHERE f.tipsterId = t.id AND f.dateId = d.id AND f.status <> :pending AND f.tipsterId IS NOT NULL
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY t.id, t.name
			""")
	List<SegmentedAggregateProjection> aggregateByTipster(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT tm.id AS dimensionId, tm.name AS dimensionName, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimTeamJpaEntity tm, DimDateJpaEntity d
			WHERE (f.team1Id = tm.id OR f.team2Id = tm.id) AND f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY tm.id, tm.name
			""")
	List<SegmentedAggregateProjection> aggregateByTeam(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT d.year AS year, d.month AS month, SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY d.year, d.month
			""")
	List<MonthlyAggregateProjection> aggregateByMonth(@Param("pending") BetStatus pending, @Param("lost") BetStatus lost,
			@Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre, @Param("live") BetType live,
			@Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT f.betType AS betType, SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount,
			       SUM(CASE WHEN f.status = :lost THEN 1L ELSE 0L END) AS lostCount,
			       SUM(CASE WHEN f.status = :voidStatus THEN 1L ELSE 0L END) AS voidCount,
			       SUM(CASE WHEN f.betType = :pre THEN 1L ELSE 0L END) AS preCount,
			       SUM(CASE WHEN f.betType = :live THEN 1L ELSE 0L END) AS liveCount,
			       AVG(f.odd) AS avgOdd, COUNT(f) AS settledCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending AND f.betType IS NOT NULL
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY f.betType
			""")
	List<BetTypeAggregateProjection> aggregateByBetType(@Param("pending") BetStatus pending,
			@Param("lost") BetStatus lost, @Param("voidStatus") BetStatus voidStatus, @Param("pre") BetType pre,
			@Param("live") BetType live, @Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT FUNCTION('make_date', d.year, d.month, d.day) AS date, SUM(f.stake) AS totalStaked,
			       SUM(f.profit) AS netProfit, COUNT(f) AS betCount
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.sportId()} IS NULL OR f.sportId = :#{#filter.sportId()})
			  AND (:#{#filter.leagueId()} IS NULL OR f.leagueId = :#{#filter.leagueId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			GROUP BY d.year, d.month, d.day
			ORDER BY d.year, d.month, d.day
			""")
	List<DailyAggregateProjection> aggregateByDay(@Param("pending") BetStatus pending,
			@Param("filter") ResolvedStatisticsFilter filter);

	@Query("""
			SELECT SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount,
			       AVG(f.odd) AS avgOdd
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND f.sportId = :#{#filter.sportId()} AND f.leagueId = :#{#filter.leagueId()}
			  AND (:#{#filter.teamId()} IS NULL OR f.team1Id = :#{#filter.teamId()} OR f.team2Id = :#{#filter.teamId()})
			  AND (:#{#filter.betType()} IS NULL OR f.betType = :#{#filter.betType()})
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			""")
	AggregateWithOddProjection aggregateForSearch(@Param("pending") BetStatus pending,
			@Param("filter") ResolvedStatisticsSearchFilter filter);

	@Query("""
			SELECT FUNCTION('make_date', d.year, d.month, d.day) AS date, f.profit AS profit
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND f.sportId = :#{#filter.sportId()} AND f.leagueId = :#{#filter.leagueId()}
			  AND (:#{#filter.teamId()} IS NULL OR f.team1Id = :#{#filter.teamId()} OR f.team2Id = :#{#filter.teamId()})
			  AND (:#{#filter.betType()} IS NULL OR f.betType = :#{#filter.betType()})
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			ORDER BY FUNCTION('make_date', d.year, d.month, d.day) ASC
			""")
	List<SettledBetPointProjection> findOrderedSettledProfits(@Param("pending") BetStatus pending,
			@Param("filter") ResolvedStatisticsSearchFilter filter);
}
