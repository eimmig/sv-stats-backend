package com.stakevault.betting.stats.domain.port.out;

import com.stakevault.betting.stats.domain.model.DimTipster;

public interface DimTipsterRepository {

	DimTipster save(DimTipster dimTipster);

	void insertIfAbsent(DimTipster dimTipster);
}
