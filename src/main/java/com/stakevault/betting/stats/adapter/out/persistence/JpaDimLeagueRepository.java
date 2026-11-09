package com.stakevault.betting.stats.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;

@Repository
public class JpaDimLeagueRepository implements DimLeagueRepository {

	private final DimLeagueSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimLeagueRepository(DimLeagueSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimLeague save(DimLeague dimLeague) {
		var saved = jpaRepository.save(new DimLeagueJpaEntity(dimLeague.id(), dimLeague.name()));
		return new DimLeague(saved.getId(), saved.getName());
	}

	@Override
	public void insertIfAbsent(DimLeague dimLeague) {
		String key = String.valueOf(dimLeague.id());
		if (knownDimensions.contains("league", key)) {
			return;
		}
		jpaRepository.insertIfAbsent(dimLeague.id(), dimLeague.name());
		knownDimensions.remember("league", key, Boolean.TRUE);
	}
}
