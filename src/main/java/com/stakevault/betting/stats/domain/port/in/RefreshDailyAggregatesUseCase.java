package com.stakevault.betting.stats.domain.port.in;

public interface RefreshDailyAggregatesUseCase {

	/**
	 * Recalcula ate {@code maxDays} dias pendentes da organizacao corrente e devolve quantos foram
	 * recalculados; os demais ficam para as rodadas seguintes.
	 */
	int refreshDirtyDays(int maxDays);
}
