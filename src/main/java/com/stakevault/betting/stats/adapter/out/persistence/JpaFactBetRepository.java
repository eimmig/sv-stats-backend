package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;

@Repository
public class JpaFactBetRepository implements FactBetRepository {

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
	public BetAggregate aggregateOverall() {
		return toAggregate(jpaRepository.aggregateOverall(BetStatus.PENDING));
	}

	@Override
	public List<SegmentedBetAggregate> aggregateBySport() {
		return jpaRepository.aggregateBySport(BetStatus.PENDING).stream().map(JpaFactBetRepository::toSegment).toList();
	}

	@Override
	public List<SegmentedBetAggregate> aggregateByMarket() {
		return jpaRepository.aggregateByMarket(BetStatus.PENDING)
				.stream()
				.map(JpaFactBetRepository::toSegment)
				.toList();
	}

	@Override
	public List<SegmentedBetAggregate> aggregateByBettingHouse() {
		return jpaRepository.aggregateByBettingHouse(BetStatus.PENDING)
				.stream()
				.map(JpaFactBetRepository::toSegment)
				.toList();
	}

	private static FactBet toDomain(FactBetJpaEntity entity) {
		return new FactBet(entity.getId(), entity.getDateId(), entity.getBettingHouseId(), entity.getSportId(),
				entity.getLeagueId(), entity.getMarketId(), entity.getTipsterId(), entity.getStake(),
				entity.getProfit(), entity.getIsWin(), entity.getStatus(), entity.getBetCount());
	}

	// SUM sobre um grupo vazio (nenhuma aposta liquidada) retorna null em SQL, nao zero.
	private static BetAggregate toAggregate(AggregateProjection projection) {
		BigDecimal totalStaked = projection.getTotalStaked() != null ? projection.getTotalStaked() : BigDecimal.ZERO;
		BigDecimal netProfit = projection.getNetProfit() != null ? projection.getNetProfit() : BigDecimal.ZERO;
		long wonCount = projection.getWonCount() != null ? projection.getWonCount() : 0L;
		long settledCount = projection.getSettledCount() != null ? projection.getSettledCount() : 0L;
		return new BetAggregate(totalStaked, netProfit, wonCount, settledCount);
	}

	private static SegmentedBetAggregate toSegment(SegmentedAggregateProjection projection) {
		return new SegmentedBetAggregate(projection.getDimensionId(), projection.getDimensionName(),
				toAggregate(projection));
	}
}
