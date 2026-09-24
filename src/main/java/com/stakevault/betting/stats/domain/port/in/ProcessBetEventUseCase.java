package com.stakevault.betting.stats.domain.port.in;

import java.util.UUID;

public interface ProcessBetEventUseCase {

	void processCreated(UUID eventId, BetCreatedEvent event);

	void processSettled(UUID eventId, BetSettledEvent event);
}
