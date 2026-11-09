package com.stakevault.betting.stats.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;

@Repository
public class JpaDimMarketRepository implements DimMarketRepository {

	private final DimMarketSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimMarketRepository(DimMarketSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimMarket save(DimMarket dimMarket) {
		var saved = jpaRepository.save(new DimMarketJpaEntity(dimMarket.id(), dimMarket.name()));
		return new DimMarket(saved.getId(), saved.getName());
	}

	@Override
	public void insertIfAbsent(DimMarket dimMarket) {
		String key = String.valueOf(dimMarket.id());
		if (knownDimensions.contains("market", key)) {
			return;
		}
		jpaRepository.insertIfAbsent(dimMarket.id(), dimMarket.name());
		knownDimensions.remember("market", key, Boolean.TRUE);
	}
}
