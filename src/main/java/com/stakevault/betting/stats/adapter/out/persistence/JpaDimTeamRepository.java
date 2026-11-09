package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;

@Repository
public class JpaDimTeamRepository implements DimTeamRepository {

	private final DimTeamSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimTeamRepository(DimTeamSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimTeam save(DimTeam dimTeam) {
		var saved = jpaRepository.save(new DimTeamJpaEntity(dimTeam.id(), dimTeam.name(), dimTeam.sportId()));
		return toDomain(saved);
	}

	@Override
	public void insertIfAbsent(DimTeam dimTeam) {
		jpaRepository.insertIfAbsent(dimTeam.id(), dimTeam.name(), dimTeam.sportId());
	}

	@Override
	public Optional<DimTeam> findByNameAndSportId(String name, UUID sportId) {
		String key = sportId + "|" + name;
		Optional<DimTeam> known = knownDimensions.find("team", key, DimTeam.class);
		if (known.isPresent()) {
			return known;
		}
		Optional<DimTeam> found = jpaRepository.findByNameAndSportId(name, sportId)
				.map(JpaDimTeamRepository::toDomain);
		found.ifPresent(team -> knownDimensions.remember("team", key, team));
		return found;
	}

	@Override
	public List<DimTeam> findBySportId(UUID sportId) {
		return jpaRepository.findBySportIdOrderByName(sportId).stream().map(JpaDimTeamRepository::toDomain).toList();
	}

	private static DimTeam toDomain(DimTeamJpaEntity entity) {
		return new DimTeam(entity.getId(), entity.getName(), entity.getSportId());
	}
}
