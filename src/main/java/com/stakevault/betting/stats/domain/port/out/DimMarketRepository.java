package com.stakevault.betting.stats.domain.port.out;

import com.stakevault.betting.stats.domain.model.DimMarket;

public interface DimMarketRepository {

	DimMarket save(DimMarket dimMarket);

	void insertIfAbsent(DimMarket dimMarket);
}
