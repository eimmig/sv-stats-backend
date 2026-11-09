package com.stakevault.betting.stats.domain.port.in;

public interface RefreshDailyAggregatesUseCase {

	/** Recalcula os dias pendentes da organizacao corrente e devolve quantos foram recalculados. */
	int refreshDirtyDays();
}
