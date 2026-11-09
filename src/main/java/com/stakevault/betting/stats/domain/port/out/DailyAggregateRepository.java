package com.stakevault.betting.stats.domain.port.out;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/** Recalculo das somas diarias por segmento da organizacao corrente (feat-036). */
public interface DailyAggregateRepository {

	/** Lock da transacao corrente para o schema; falso se outra replica ja esta recalculando. */
	boolean tryLock();

	/** Remove e devolve os dias marcados como desatualizados. */
	List<LocalDate> takeDirtyDays();

	/** Apaga e recalcula, a partir de fact_bet, as somas dos dias informados. */
	void rebuild(Collection<LocalDate> days);
}
