package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimLeague;

public interface DimLeagueRepository {

	DimLeague save(DimLeague dimLeague);

	void insertIfAbsent(DimLeague dimLeague);

	boolean existsById(UUID id);
}
