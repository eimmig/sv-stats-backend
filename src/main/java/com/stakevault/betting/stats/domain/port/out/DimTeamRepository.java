package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimTeam;

public interface DimTeamRepository {

	DimTeam save(DimTeam dimTeam);

	// Sem id vindo do evento (team1/team2 sao texto livre em bets-service) - localizada pela
	// chave natural composta (name, sportId) e criada sob demanda na primeira aposta que a
	// referencia. sportId faz parte da chave porque o mesmo nome de time pode existir em
	// esportes diferentes (feat-013).
	Optional<DimTeam> findByNameAndSportId(String name, UUID sportId);

	// GET /api/v1/statistics/teams (feat-013) - autocomplete escopado por esporte.
	List<DimTeam> findBySportId(UUID sportId);
}
