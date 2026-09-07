package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimSport;

public interface DimSportRepository {

	DimSport save(DimSport dimSport);

	boolean existsById(UUID id);
}
