package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;

interface FactBetSpringDataRepository extends JpaRepository<FactBetJpaEntity, UUID> {

	// RF11/RN08: predicados opcionais "(:#{...} IS NULL OR ...)" pros UUIDs, agrupados num unico
	// parametro SpEL (ResolvedStatisticsFilter) - achado real do SonarCloud (java:S107, mais de 7
	// parametros) na primeira tentativa com um @Param por campo. O intervalo de data usa
	// limites-sentinela (JpaFactBetRepository substitui from/to null por MIN/MAX antes de montar
	// o filtro resolvido) em vez de outro "IS NULL OR" - Postgres nao consegue inferir o tipo de
	// um parametro null usado so dentro de CAST/FUNCTION, achado real durante a implementacao.
	// FUNCTION('make_date', ...) e Postgres-especifico, unico banco alvo do projeto. lost/voidStatus/
	// pre/live SEMPRE parametros @Param tipados (nunca literal de string solto tipo "f.status =
	// 'LOST'") - achado do plan review de epic-014: o codebase inteiro ja evita literal de enum em
	// JPQL (so usa parametro, ver :pending), pra garantir que o AttributeConverter seja aplicado.
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

	// 6o segmento (epic-014): so 2 buckets fixos (PRE/LIVE) - f.betType IS NOT NULL exclui apostas
	// sem classificacao de qualquer um dos dois. betType nunca e filtro de negocio, so o campo de
	// agrupamento em si.
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

	// epic-016 (GET /api/v1/statistics/daily): shape enxuto (so totalStaked/netProfit/betCount -
	// a resposta HTTP nao expoe wonCount/lostCount/avgOdd/etc, entao a query nao os calcula).
	// FUNCTION('make_date', d.year, d.month, d.day) no SELECT agrupado pelas 3 colunas cruas -
	// expressao deterministica so das colunas do GROUP BY, permitido por SQL padrao mesmo sem
	// repetir a expressao no GROUP BY; combinacao provada pelo teste de integracao real.
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

	// epic-011: sportId/leagueId sempre presentes (igualdade direta, sem "IS NULL OR" - o filtro
	// de dominio garante isso via requireNonNull); teamId casa contra qualquer um dos 2 lados.
	@Query("""
			SELECT SUM(f.stake) AS totalStaked, SUM(f.profit) AS netProfit,
			       SUM(CASE WHEN f.isWin = true THEN 1L ELSE 0L END) AS wonCount, COUNT(f) AS settledCount,
			       AVG(f.odd) AS avgOdd
			FROM FactBetJpaEntity f, DimDateJpaEntity d
			WHERE f.dateId = d.id AND f.status <> :pending
			  AND f.sportId = :#{#filter.sportId()} AND f.leagueId = :#{#filter.leagueId()}
			  AND (:#{#filter.teamId()} IS NULL OR f.team1Id = :#{#filter.teamId()} OR f.team2Id = :#{#filter.teamId()})
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
			  AND (:#{#filter.bettingHouseId()} IS NULL OR f.bettingHouseId = :#{#filter.bettingHouseId()})
			  AND (:#{#filter.marketId()} IS NULL OR f.marketId = :#{#filter.marketId()})
			  AND (:#{#filter.tipsterId()} IS NULL OR f.tipsterId = :#{#filter.tipsterId()})
			  AND FUNCTION('make_date', d.year, d.month, d.day) BETWEEN :#{#filter.from()} AND :#{#filter.to()}
			ORDER BY FUNCTION('make_date', d.year, d.month, d.day) ASC
			""")
	List<SettledBetPointProjection> findOrderedSettledProfits(@Param("pending") BetStatus pending,
			@Param("filter") ResolvedStatisticsSearchFilter filter);
}
