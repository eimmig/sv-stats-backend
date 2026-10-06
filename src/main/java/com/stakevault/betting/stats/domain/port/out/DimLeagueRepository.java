package com.stakevault.betting.stats.domain.port.out;

import com.stakevault.betting.stats.domain.model.DimLeague;

public interface DimLeagueRepository {

	DimLeague save(DimLeague dimLeague);

	void insertIfAbsent(DimLeague dimLeague);
}
