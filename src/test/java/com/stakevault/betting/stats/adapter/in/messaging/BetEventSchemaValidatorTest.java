package com.stakevault.betting.stats.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class BetEventSchemaValidatorTest {

	private final BetEventSchemaValidator validator = new BetEventSchemaValidator();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private JsonNode validBetCreated() throws Exception {
		String json = """
				{
				  "eventId": "%s",
				  "eventType": "BetCreated",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "tenantId": "acme",
				  "userId": "%s",
				  "payload": {
				    "betId": "%s",
				    "bettingHouseId": "%s",
				    "sportId": "%s",
				    "leagueId": "%s",
				    "marketId": "%s",
				    "tipsterId": null,
				    "ticketNumber": null,
				    "team1": null,
				    "team2": null,
				    "description": null,
				    "betType": null,
				    "playType": null,
				    "stake": 100.0,
				    "odd": 1.5,
				    "status": "pending",
				    "betDate": "%s"
				  }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
		return objectMapper.readTree(json);
	}

	@Test
	void shouldAcceptAValidBetCreatedEvent() {
		assertThatNoException().isThrownBy(() -> validator.validate("BetCreated", validBetCreated()));
	}

	@Test
	void shouldRejectABetCreatedEventMissingRequiredFields() throws Exception {
		JsonNode incomplete = objectMapper.readTree("""
				{
				  "eventId": "%s",
				  "eventType": "BetCreated",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "tenantId": "acme",
				  "userId": "%s",
				  "payload": { "betId": "not-a-uuid" }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID()));

		assertThatThrownBy(() -> validator.validate("BetCreated", incomplete))
				.isInstanceOf(InvalidBetEventException.class);
	}

	@Test
	void shouldRejectAnUnknownEventType() throws Exception {
		JsonNode node = objectMapper.readTree("{}");

		assertThatThrownBy(() -> validator.validate("SomethingElse", node))
				.isInstanceOf(UnknownBetEventTypeException.class);
	}
}
