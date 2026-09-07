package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;

@Repository
public class JpaDimTipsterRepository implements DimTipsterRepository {

	private final DimTipsterSpringDataRepository jpaRepository;

	public JpaDimTipsterRepository(DimTipsterSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimTipster save(DimTipster dimTipster) {
		var saved = jpaRepository.save(new DimTipsterJpaEntity(dimTipster.id(), dimTipster.name()));
		return new DimTipster(saved.getId(), saved.getName());
	}

	@Override
	public boolean existsById(UUID id) {
		return jpaRepository.existsById(id);
	}
}
