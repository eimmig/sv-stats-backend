package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;

@Repository
public class JpaDimLeagueRepository implements DimLeagueRepository {

	private final DimLeagueSpringDataRepository jpaRepository;

	public JpaDimLeagueRepository(DimLeagueSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimLeague save(DimLeague dimLeague) {
		var saved = jpaRepository.save(new DimLeagueJpaEntity(dimLeague.id(), dimLeague.name()));
		return new DimLeague(saved.getId(), saved.getName());
	}

	@Override
	public boolean existsById(UUID id) {
		return jpaRepository.existsById(id);
	}
}
