package com.stakevault.betting.stats.domain.model;

import java.time.Instant;
import java.util.UUID;

// Controle tecnico de idempotencia do consumo de evento (feat-003) - nao participa do esquema
// estrela (nem fato nem dimensao). eventId e unico no banco (constraint real, nao so checagem
// em nivel de aplicacao).
public record ProcessedEvent(UUID id, UUID eventId, Instant processedAt) {
}
