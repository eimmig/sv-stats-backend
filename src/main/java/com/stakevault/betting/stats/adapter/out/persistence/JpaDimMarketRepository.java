package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;

@Repository
public class JpaDimMarketRepository implements DimMarketRepository {

	private final DimMarketSpringDataRepository jpaRepository;

	public JpaDimMarketRepository(DimMarketSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimMarket save(DimMarket dimMarket) {
		var saved = jpaRepository.save(new DimMarketJpaEntity(dimMarket.id(), dimMarket.name()));
		return new DimMarket(saved.getId(), saved.getName());
	}

	@Override
	public boolean existsById(UUID id) {
		return jpaRepository.existsById(id);
	}
}
