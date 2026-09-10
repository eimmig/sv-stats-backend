package com.stakevault.betting.stats.domain.port.out;

import java.util.Optional;

import com.stakevault.betting.stats.domain.model.DimTeam;

public interface DimTeamRepository {

	DimTeam save(DimTeam dimTeam);

	// Sem id vindo do evento (team1/team2 sao texto livre em bets-service) - localizada pela
	// chave natural (name) e criada sob demanda na primeira aposta que a referencia.
	Optional<DimTeam> findByName(String name);
}
