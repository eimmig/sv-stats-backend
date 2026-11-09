package com.stakevault.betting.stats.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;

@Repository
public class JpaDimBettingHouseRepository implements DimBettingHouseRepository {

	private final DimBettingHouseSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimBettingHouseRepository(DimBettingHouseSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimBettingHouse save(DimBettingHouse dimBettingHouse) {
		var saved = jpaRepository.save(new DimBettingHouseJpaEntity(dimBettingHouse.id(), dimBettingHouse.name()));
		return new DimBettingHouse(saved.getId(), saved.getName());
	}

	@Override
	public void insertIfAbsent(DimBettingHouse dimBettingHouse) {
		String key = String.valueOf(dimBettingHouse.id());
		if (knownDimensions.contains("house", key)) {
			return;
		}
		jpaRepository.insertIfAbsent(dimBettingHouse.id(), dimBettingHouse.name());
		knownDimensions.remember("house", key, Boolean.TRUE);
	}
}
