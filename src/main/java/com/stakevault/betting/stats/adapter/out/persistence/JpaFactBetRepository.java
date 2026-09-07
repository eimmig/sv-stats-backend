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
		return toDomain(jpaRepository.save(new FactBetJpaEntity(factBet)));
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
