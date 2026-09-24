package com.stakevault.betting.stats.domain.model;

import java.time.Instant;
import java.util.UUID;

public record ProcessedEvent(UUID id, UUID eventId, Instant processedAt) {
}
