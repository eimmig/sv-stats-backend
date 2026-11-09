package com.stakevault.betting.stats.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;

@Repository
public class JpaDimTipsterRepository implements DimTipsterRepository {

	private final DimTipsterSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimTipsterRepository(DimTipsterSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimTipster save(DimTipster dimTipster) {
		var saved = jpaRepository.save(new DimTipsterJpaEntity(dimTipster.id(), dimTipster.name()));
		return new DimTipster(saved.getId(), saved.getName());
	}

	@Override
	public void insertIfAbsent(DimTipster dimTipster) {
		String key = String.valueOf(dimTipster.id());
		if (knownDimensions.contains("tipster", key)) {
			return;
		}
		jpaRepository.insertIfAbsent(dimTipster.id(), dimTipster.name());
		knownDimensions.remember("tipster", key, Boolean.TRUE);
	}
}
