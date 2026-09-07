package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;

public interface DimBettingHouseRepository {

	DimBettingHouse save(DimBettingHouse dimBettingHouse);

	boolean existsById(UUID id);
}
