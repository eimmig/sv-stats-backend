package com.stakevault.betting.stats.domain.port.out;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;

public interface DimBettingHouseRepository {

	DimBettingHouse save(DimBettingHouse dimBettingHouse);

	void insertIfAbsent(DimBettingHouse dimBettingHouse);
}
