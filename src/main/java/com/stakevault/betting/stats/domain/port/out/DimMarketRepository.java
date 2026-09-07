package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimMarket;

public interface DimMarketRepository {

	DimMarket save(DimMarket dimMarket);

	boolean existsById(UUID id);
}
