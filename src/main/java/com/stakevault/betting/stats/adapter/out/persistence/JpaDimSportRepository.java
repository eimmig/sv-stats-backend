package com.stakevault.betting.stats.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;

@Repository
public class JpaDimSportRepository implements DimSportRepository {

	private final DimSportSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimSportRepository(DimSportSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimSport save(DimSport dimSport) {
		var saved = jpaRepository.save(new DimSportJpaEntity(dimSport.id(), dimSport.name()));
		return new DimSport(saved.getId(), saved.getName());
	}

	@Override
	public void insertIfAbsent(DimSport dimSport) {
		String key = String.valueOf(dimSport.id());
		if (knownDimensions.contains("sport", key)) {
			return;
		}
		jpaRepository.insertIfAbsent(dimSport.id(), dimSport.name());
		knownDimensions.remember("sport", key, Boolean.TRUE);
	}
}
