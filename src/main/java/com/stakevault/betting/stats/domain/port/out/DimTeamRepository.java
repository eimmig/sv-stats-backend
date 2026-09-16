package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimTeam;

public interface DimTeamRepository {

	DimTeam save(DimTeam dimTeam);

	boolean existsById(UUID id);

	// Chave natural composta (name, sportId) - localizada primeiro em resolveTeam, tem
	// precedencia sobre o id vindo do evento (ver DimensionResolver.resolveTeam) para nunca
	// violar UNIQUE(name, sport_id) quando um time ja resolvido localmente (id gerado antes do
	// catalogo TEAM de bets-service existir) reaparece com o id real do catalogo.
	Optional<DimTeam> findByNameAndSportId(String name, UUID sportId);

	// GET /api/v1/statistics/teams - autocomplete escopado por esporte.
	List<DimTeam> findBySportId(UUID sportId);
}
