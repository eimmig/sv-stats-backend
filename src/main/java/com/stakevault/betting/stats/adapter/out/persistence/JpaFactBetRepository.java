package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.FactBet;
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
				.map(existing -> applyTo(existing, factBet))
				.orElseGet(() -> new FactBetJpaEntity(factBet));
		return toDomain(jpaRepository.save(entity));
	}

	private static FactBetJpaEntity applyTo(FactBetJpaEntity entity, FactBet factBet) {
		entity.setDateId(factBet.dateId());
		entity.setBettingHouseId(factBet.bettingHouseId());
		entity.setSportId(factBet.sportId());
		entity.setLeagueId(factBet.leagueId());
		entity.setMarketId(factBet.marketId());
		entity.setTipsterId(factBet.tipsterId());
		entity.setStake(factBet.stake());
		entity.setProfit(factBet.profit());
		entity.setIsWin(factBet.isWin());
		entity.setStatus(factBet.status());
		entity.setBetCount(factBet.betCount());
		return entity;
	}

	@Override
	public Optional<FactBet> findById(UUID id) {
		return jpaRepository.findById(id).map(JpaFactBetRepository::toDomain);
	}

	private static FactBet toDomain(FactBetJpaEntity entity) {
		return new FactBet(entity.getId(), entity.getDateId(), entity.getBettingHouseId(), entity.getSportId(),
				entity.getLeagueId(), entity.getMarketId(), entity.getTipsterId(), entity.getStake(),
				entity.getProfit(), entity.getIsWin(), entity.getStatus(), entity.getBetCount());
	}
}
