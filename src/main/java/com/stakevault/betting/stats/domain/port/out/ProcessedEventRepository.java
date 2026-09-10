package com.stakevault.betting.stats.domain.port.out;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.ProcessedEvent;

public interface ProcessedEventRepository {

	ProcessedEvent save(ProcessedEvent processedEvent);

	boolean existsByEventId(UUID eventId);
}
