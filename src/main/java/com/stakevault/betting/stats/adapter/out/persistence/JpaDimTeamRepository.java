package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;

@Repository
public class JpaDimTeamRepository implements DimTeamRepository {

	private final DimTeamSpringDataRepository jpaRepository;

	public JpaDimTeamRepository(DimTeamSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimTeam save(DimTeam dimTeam) {
		var saved = jpaRepository.save(new DimTeamJpaEntity(dimTeam.id(), dimTeam.name()));
		return toDomain(saved);
	}

	@Override
	public Optional<DimTeam> findByName(String name) {
		return jpaRepository.findByName(name).map(JpaDimTeamRepository::toDomain);
	}

	private static DimTeam toDomain(DimTeamJpaEntity entity) {
		return new DimTeam(entity.getId(), entity.getName());
	}
}
