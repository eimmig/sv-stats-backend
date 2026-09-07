package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;

@Repository
public class JpaDimSportRepository implements DimSportRepository {

	private final DimSportSpringDataRepository jpaRepository;

	public JpaDimSportRepository(DimSportSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimSport save(DimSport dimSport) {
		var saved = jpaRepository.save(new DimSportJpaEntity(dimSport.id(), dimSport.name()));
		return new DimSport(saved.getId(), saved.getName());
	}

	@Override
	public boolean existsById(UUID id) {
		return jpaRepository.existsById(id);
	}
}
