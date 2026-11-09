package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

/**
 * Somas diarias por segmento (agg_daily_segment, feat-036). As leituras somam os dias do intervalo
 * e devolvem as mesmas projecoes das consultas de fact_bet, com a media da odd e a implicita
 * recompostas de soma e contagem. O recalculo reproduz as regras de FactBetSpringDataRepository:
 * pendentes fora, profit nulo como zero nos somatorios, tipster e tipo de aposta so quando
 * preenchidos e, por time, a aposta entra para team1 e team2 uma vez cada (uma vez so quando sao
 * o mesmo time).
 */
interface DailySegmentAggregateSpringDataRepository extends Repository<FactBetJpaEntity, UUID> {

	String SUMS = """
			CAST(COALESCE(SUM(a.total_staked), 0) AS numeric) AS "totalStaked",
			CAST(COALESCE(SUM(a.net_profit), 0) AS numeric) AS "netProfit",
			CAST(COALESCE(SUM(a.won_count), 0) AS bigint) AS "wonCount",
			CAST(COALESCE(SUM(a.lost_count), 0) AS bigint) AS "lostCount",
			CAST(COALESCE(SUM(a.void_count), 0) AS bigint) AS "voidCount",
			CAST(COALESCE(SUM(a.pre_count), 0) AS bigint) AS "preCount",
			CAST(COALESCE(SUM(a.live_count), 0) AS bigint) AS "liveCount",
			SUM(a.odd_sum) / NULLIF(SUM(a.odd_count), 0) AS "avgOdd",
			CAST(COALESCE(SUM(a.settled_count), 0) AS bigint) AS "settledCount",
			SUM(a.implied_sum) / NULLIF(SUM(a.decided_with_odd_count), 0) AS "impliedProbability",
			CAST(COALESCE(SUM(a.decided_with_odd_count), 0) AS bigint) AS "decidedWithOddCount",
			CAST(COALESCE(SUM(a.profit_squared_sum), 0) AS numeric) AS "profitSquaredSum",
			CAST(COALESCE(SUM(a.stake_squared_sum), 0) AS numeric) AS "stakeSquaredSum",
			CAST(COALESCE(SUM(a.profit_stake_sum), 0) AS numeric) AS "profitStakeSum"
			""";

	String SEGMENT_SELECT = "SELECT dim.id AS \"dimensionId\", dim.name AS \"dimensionName\", " + SUMS;

	String SEGMENT_WHERE = """
			ON dim.id = CAST(a.dimension_key AS uuid)
			WHERE a.segment = :segment AND a.day BETWEEN :from AND :to
			GROUP BY dim.id, dim.name
			""";

	@Query(value = "SELECT EXISTS (SELECT 1 FROM agg_dirty_day WHERE day BETWEEN :from AND :to)", nativeQuery = true)
	boolean hasDirtyDay(@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query(value = "SELECT " + SUMS + """
			FROM agg_daily_segment a WHERE a.segment = 'OVERALL' AND a.day BETWEEN :from AND :to
			""", nativeQuery = true)
	AggregateProjection sumOverall(@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_sport dim " + SEGMENT_WHERE, nativeQuery = true)
	List<SegmentedAggregateProjection> sumBySport(@Param("segment") String segment, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_market dim " + SEGMENT_WHERE,
			nativeQuery = true)
	List<SegmentedAggregateProjection> sumByMarket(@Param("segment") String segment, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_betting_house dim " + SEGMENT_WHERE,
			nativeQuery = true)
	List<SegmentedAggregateProjection> sumByBettingHouse(@Param("segment") String segment,
			@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_league dim " + SEGMENT_WHERE,
			nativeQuery = true)
	List<SegmentedAggregateProjection> sumByLeague(@Param("segment") String segment, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_tipster dim " + SEGMENT_WHERE,
			nativeQuery = true)
	List<SegmentedAggregateProjection> sumByTipster(@Param("segment") String segment, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query(value = SEGMENT_SELECT + "FROM agg_daily_segment a JOIN dim_team dim " + SEGMENT_WHERE, nativeQuery = true)
	List<SegmentedAggregateProjection> sumByTeam(@Param("segment") String segment, @Param("from") LocalDate from,
			@Param("to") LocalDate to);

	@Query(value = "SELECT UPPER(a.dimension_key) AS \"betType\", " + SUMS + """
			FROM agg_daily_segment a WHERE a.segment = 'BET_TYPE' AND a.day BETWEEN :from AND :to
			GROUP BY a.dimension_key
			""", nativeQuery = true)
	List<BetTypeAggregateProjection> sumByBetType(@Param("from") LocalDate from, @Param("to") LocalDate to);

	@Query(value = """
			SELECT CAST(EXTRACT(YEAR FROM a.day) AS integer) AS "year",
			       CAST(EXTRACT(MONTH FROM a.day) AS integer) AS "month",
			""" + SUMS + """
			FROM agg_daily_segment a WHERE a.segment = 'OVERALL' AND a.day BETWEEN :from AND :to
			GROUP BY EXTRACT(YEAR FROM a.day), EXTRACT(MONTH FROM a.day)
			ORDER BY 1, 2
			""", nativeQuery = true)
	List<MonthlyAggregateProjection> sumByMonth(@Param("from") LocalDate from, @Param("to") LocalDate to);

	/** Um agendador por replica: so quem pega o lock recalcula este schema nesta rodada. */
	@Query(value = "SELECT pg_try_advisory_xact_lock(hashtext(current_schema()))", nativeQuery = true)
	boolean tryLockSchema();

	/**
	 * Marca gravada por transacao ainda aberta nao aparece aqui e continua na tabela para a rodada
	 * seguinte; o recalculo roda depois, em outra instrucao, e ja enxerga o que foi confirmado antes.
	 */
	@Query(value = "DELETE FROM agg_dirty_day RETURNING day", nativeQuery = true)
	List<LocalDate> takeDirtyDays();

	@Modifying
	@Query(value = "DELETE FROM agg_daily_segment WHERE day IN (:days)", nativeQuery = true)
	int deleteDays(@Param("days") Collection<LocalDate> days);

	@Modifying
	@Query(value = """
			INSERT INTO agg_daily_segment (day, segment, dimension_key, total_staked, net_profit, won_count,
			                               lost_count, void_count, pre_count, live_count, settled_count, odd_sum,
			                               odd_count, implied_sum, decided_with_odd_count, profit_squared_sum,
			                               stake_squared_sum, profit_stake_sum)
			SELECT b.day, s.segment, s.dimension_key,
			       SUM(b.stake), COALESCE(SUM(b.profit), 0),
			       SUM(CASE WHEN b.is_win = true THEN 1 ELSE 0 END),
			       SUM(CASE WHEN b.status = 'lost' THEN 1 ELSE 0 END),
			       SUM(CASE WHEN b.status = 'void' THEN 1 ELSE 0 END),
			       SUM(CASE WHEN b.bet_type = 'pre' THEN 1 ELSE 0 END),
			       SUM(CASE WHEN b.bet_type = 'live' THEN 1 ELSE 0 END),
			       COUNT(*), COALESCE(SUM(b.odd), 0), COUNT(b.odd),
			       COALESCE(SUM(CASE WHEN (b.is_win = true OR b.status = 'lost') AND b.odd > 0
			                         THEN 1.0 / b.odd END), 0),
			       SUM(CASE WHEN (b.is_win = true OR b.status = 'lost') AND b.odd > 0 THEN 1 ELSE 0 END),
			       SUM(COALESCE(b.profit, 0) * COALESCE(b.profit, 0)), SUM(b.stake * b.stake),
			       SUM(COALESCE(b.profit, 0) * b.stake)
			FROM (SELECT f.*, make_date(d.year, d.month, d.day) AS day
			      FROM fact_bet f JOIN dim_date d ON d.id = f.date_id
			      WHERE f.status <> 'pending' AND make_date(d.year, d.month, d.day) IN (:days)) b
			CROSS JOIN LATERAL (VALUES ('OVERALL', ''),
			                           ('SPORT', CAST(b.sport_id AS varchar)),
			                           ('MARKET', CAST(b.market_id AS varchar)),
			                           ('HOUSE', CAST(b.betting_house_id AS varchar)),
			                           ('LEAGUE', CAST(b.league_id AS varchar)),
			                           ('TIPSTER', CAST(b.tipster_id AS varchar)),
			                           ('BET_TYPE', CAST(b.bet_type AS varchar)),
			                           ('TEAM', CAST(b.team1_id AS varchar)),
			                           ('TEAM', CASE WHEN b.team2_id IS DISTINCT FROM b.team1_id
			                                         THEN CAST(b.team2_id AS varchar) END)) s (segment, dimension_key)
			WHERE s.dimension_key IS NOT NULL
			GROUP BY b.day, s.segment, s.dimension_key
			""", nativeQuery = true)
	int insertDays(@Param("days") Collection<LocalDate> days);
}
