package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimTipster;

public interface DimTipsterRepository {

	DimTipster save(DimTipster dimTipster);

	boolean existsById(UUID id);
}
