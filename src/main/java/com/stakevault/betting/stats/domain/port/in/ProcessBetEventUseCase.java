package com.stakevault.betting.stats.domain.port.in;

import java.util.UUID;

public interface ProcessBetEventUseCase {

	// eventId (nao o betId dentro do evento) - chave de idempotencia, ver PROCESSED_EVENT.
	// Verificado e marcado como processado na MESMA transacao da escrita em FACT_BET.
	void processCreated(UUID eventId, BetCreatedEvent event);

	void processSettled(UUID eventId, BetSettledEvent event);
}
