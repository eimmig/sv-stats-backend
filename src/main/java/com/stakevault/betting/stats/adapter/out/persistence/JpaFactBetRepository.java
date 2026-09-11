package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.DailyBetAggregate;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.MonthlyBetAggregate;
import com.stakevault.betting.stats.domain.model.SearchAggregate;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.SettledBetPoint;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;

@Repository
public class JpaFactBetRepository implements FactBetRepository {

	// Limites-sentinela em vez de null: Postgres nao consegue inferir o tipo de um parametro
	// null usado so dentro de CAST/FUNCTION (achado real, ver plan_review de feat-006) - fora de
	// qualquer intervalo real de aposta, sem risco de overflow de driver como LocalDate.MIN/MAX.
	private static final LocalDate MIN_DATE = LocalDate.of(1900, 1, 1);
	private static final LocalDate MAX_DATE = LocalDate.of(2999, 12, 31);

	private final FactBetSpringDataRepository jpaRepository;

	public JpaFactBetRepository(FactBetSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public FactBet save(FactBet factBet) {
		FactBetJpaEntity entity = jpaRepository.findById(factBet.id())
				.map(existing -> {
					existing.applyFrom(factBet);
					return existing;
				})
				.orElseGet(() -> new FactBetJpaEntity(factBet));
		return toDomain(jpaRepository.save(entity));
	}

	@Override
	public Optional<FactBet> findById(UUID id) {
		return jpaRepository.findById(id).map(JpaFactBetRepository::toDomain);
	}

	@Override
	public BetAggregate aggregateOverall(StatisticsFilter filter) {
		return toAggregate(jpaRepository.aggregateOverall(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID,
				BetType.PRE, BetType.LIVE, resolve(filter)));
	}

	@Override
	public List<SegmentedBetAggregate> aggregateBySport(StatisticsFilter filter) {
		return jpaRepository.aggregateBySport(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID, BetType.PRE,
				BetType.LIVE, resolve(filter))
				.stream()
				.map(JpaFactBetRepository::toSegment)
				.toList();
	}

	@Override
	public List<SegmentedBetAggregate> aggregateByMarket(StatisticsFilter filter) {
		return jpaRepository.aggregateByMarket(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID, BetType.PRE,
				BetType.LIVE, resolve(filter))
				.stream()
				.map(JpaFactBetRepository::toSegment)
				.toList();
	}

	@Override
	public List<SegmentedBetAggregate> aggregateByBettingHouse(StatisticsFilter filter) {
		return jpaRepository.aggregateByBettingHouse(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID, BetType.PRE,
				BetType.LIVE, resolve(filter))
				.stream()
				.map(JpaFactBetRepository::toSegment)
				.toList();
	}

	@Override
	public List<MonthlyBetAggregate> aggregateByMonth(StatisticsFilter filter) {
		return jpaRepository.aggregateByMonth(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID, BetType.PRE,
				BetType.LIVE, resolve(filter))
				.stream()
				.map(projection -> new MonthlyBetAggregate(projection.getYear(), projection.getMonth(),
						toAggregate(projection)))
				.toList();
	}

	// 6o segmento (epic-014) - so 2 buckets fixos (PRE/LIVE), apostas sem betType classificado
	// ficam de fora dos dois (WHERE f.betType IS NOT NULL na query). dimensionId/dimensionName =
	// o proprio valor do enum (.name()), unico segmento sem uuid de catalogo por tras.
	@Override
	public List<SegmentedBetAggregate> aggregateByBetType(StatisticsFilter filter) {
		return jpaRepository.aggregateByBetType(BetStatus.PENDING, BetStatus.LOST, BetStatus.VOID, BetType.PRE,
				BetType.LIVE, resolve(filter))
				.stream()
				.map(JpaFactBetRepository::toBetTypeSegment)
				.toList();
	}

	@Override
	public List<DailyBetAggregate> aggregateByDay(StatisticsFilter filter) {
		return jpaRepository.aggregateByDay(BetStatus.PENDING, resolve(filter))
				.stream()
				.map(JpaFactBetRepository::toDailyAggregate)
				.toList();
	}

	@Override
	public SearchAggregate aggregateForSearch(StatisticsSearchFilter filter) {
		AggregateWithOddProjection projection = jpaRepository.aggregateForSearch(BetStatus.PENDING, resolve(filter));
		BigDecimal totalStaked = projection.getTotalStaked() != null ? projection.getTotalStaked() : BigDecimal.ZERO;
		BigDecimal netProfit = projection.getNetProfit() != null ? projection.getNetProfit() : BigDecimal.ZERO;
		long wonCount = projection.getWonCount() != null ? projection.getWonCount() : 0L;
		long settledCount = projection.getSettledCount() != null ? projection.getSettledCount() : 0L;
		return new SearchAggregate(totalStaked, netProfit, wonCount, settledCount, projection.getAvgOdd());
	}

	@Override
	public List<SettledBetPoint> findOrderedSettledProfits(StatisticsSearchFilter filter) {
		return jpaRepository.findOrderedSettledProfits(BetStatus.PENDING, resolve(filter))
				.stream()
				.map(projection -> new SettledBetPoint(projection.getDate(), projection.getProfit()))
				.toList();
	}

	private static FactBet toDomain(FactBetJpaEntity entity) {
		return new FactBet(entity.getId(), entity.getDateId(), entity.getBettingHouseId(), entity.getSportId(),
				entity.getLeagueId(), entity.getMarketId(), entity.getTipsterId(), entity.getTeam1Id(),
				entity.getTeam2Id(), entity.getStake(), entity.getOdd(), entity.getProfit(), entity.getIsWin(),
				entity.getStatus(), entity.getBetType(), entity.getBetCount());
	}

	// SUM/COUNT sobre um grupo vazio (nenhuma aposta liquidada) retorna null em SQL, nao zero -
	// avgOdd e a excecao deliberada (fica null, nunca coalescido pra ZERO, mesmo tratamento ja
	// usado por SearchAggregate.avgOdd).
	private static BetAggregate toAggregate(AggregateProjection projection) {
		BigDecimal totalStaked = projection.getTotalStaked() != null ? projection.getTotalStaked() : BigDecimal.ZERO;
		BigDecimal netProfit = projection.getNetProfit() != null ? projection.getNetProfit() : BigDecimal.ZERO;
		long wonCount = projection.getWonCount() != null ? projection.getWonCount() : 0L;
		long lostCount = projection.getLostCount() != null ? projection.getLostCount() : 0L;
		long voidCount = projection.getVoidCount() != null ? projection.getVoidCount() : 0L;
		long preCount = projection.getPreCount() != null ? projection.getPreCount() : 0L;
		long liveCount = projection.getLiveCount() != null ? projection.getLiveCount() : 0L;
		long settledCount = projection.getSettledCount() != null ? projection.getSettledCount() : 0L;
		return new BetAggregate(totalStaked, netProfit, wonCount, lostCount, voidCount, preCount, liveCount,
				projection.getAvgOdd(), settledCount);
	}

	private static SegmentedBetAggregate toSegment(SegmentedAggregateProjection projection) {
		return new SegmentedBetAggregate(projection.getDimensionId().toString(), projection.getDimensionName(),
				toAggregate(projection));
	}

	// SUM/COUNT sobre um grupo vazio nunca ocorre aqui (a linha so existe se GROUP BY produziu
	// pelo menos 1 aposta liquidada naquele dia) - null-safety mantida por simetria com toAggregate.
	private static DailyBetAggregate toDailyAggregate(DailyAggregateProjection projection) {
		BigDecimal totalStaked = projection.getTotalStaked() != null ? projection.getTotalStaked() : BigDecimal.ZERO;
		BigDecimal netProfit = projection.getNetProfit() != null ? projection.getNetProfit() : BigDecimal.ZERO;
		long betCount = projection.getBetCount() != null ? projection.getBetCount() : 0L;
		return new DailyBetAggregate(projection.getDate(), totalStaked, netProfit, betCount);
	}

	private static SegmentedBetAggregate toBetTypeSegment(BetTypeAggregateProjection projection) {
		return new SegmentedBetAggregate(projection.getBetType().name(), projection.getBetType().name(),
				toAggregate(projection));
	}

	private static LocalDate from(StatisticsFilter filter) {
		return filter.from() != null ? filter.from() : MIN_DATE;
	}

	private static LocalDate to(StatisticsFilter filter) {
		return filter.to() != null ? filter.to() : MAX_DATE;
	}

	// Agrupa os 7 campos de filtro num unico parametro de @Query (SpEL) - achado real do
	// SonarCloud (java:S107, mais de 7 parametros por metodo) quando cada campo era um @Param.
	private static ResolvedStatisticsFilter resolve(StatisticsFilter filter) {
		return new ResolvedStatisticsFilter(filter.bettingHouseId(), filter.sportId(), filter.leagueId(),
				filter.marketId(), filter.tipsterId(), from(filter), to(filter));
	}

	private static LocalDate from(StatisticsSearchFilter filter) {
		return filter.from() != null ? filter.from() : MIN_DATE;
	}

	private static LocalDate to(StatisticsSearchFilter filter) {
		return filter.to() != null ? filter.to() : MAX_DATE;
	}

	private static ResolvedStatisticsSearchFilter resolve(StatisticsSearchFilter filter) {
		return new ResolvedStatisticsSearchFilter(filter.sportId(), filter.leagueId(), filter.teamId(),
				filter.bettingHouseId(), filter.marketId(), filter.tipsterId(), from(filter), to(filter));
	}
}
