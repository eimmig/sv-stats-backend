package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimTeam;

public interface DimTeamRepository {

	DimTeam save(DimTeam dimTeam);

	boolean existsById(UUID id);

	Optional<DimTeam> findByNameAndSportId(String name, UUID sportId);

	List<DimTeam> findBySportId(UUID sportId);
}
