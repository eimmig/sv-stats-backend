package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;

@Repository
public class JpaDimBettingHouseRepository implements DimBettingHouseRepository {

	private final DimBettingHouseSpringDataRepository jpaRepository;

	public JpaDimBettingHouseRepository(DimBettingHouseSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimBettingHouse save(DimBettingHouse dimBettingHouse) {
		var saved = jpaRepository.save(new DimBettingHouseJpaEntity(dimBettingHouse.id(), dimBettingHouse.name()));
		return new DimBettingHouse(saved.getId(), saved.getName());
	}

	@Override
	public boolean existsById(UUID id) {
		return jpaRepository.existsById(id);
	}
}
