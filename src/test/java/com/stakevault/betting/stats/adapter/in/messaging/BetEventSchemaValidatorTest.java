package com.stakevault.betting.stats.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

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
				    "bettingHouseName": "House",
				    "sportId": "%s",
				    "sportName": "Sport",
				    "leagueId": "%s",
				    "leagueName": "League",
				    "marketId": "%s",
				    "marketName": "Market",
				    "tipsterId": null,
				    "tipsterName": null,
				    "ticketNumber": null,
				    "team1Id": "%s",
				    "team1": "Real Madrid",
				    "team2Id": "%s",
				    "team2": "Barcelona",
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
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), Instant.now());
		return objectMapper.readTree(json);
	}

	private JsonNode validBetSettled() throws Exception {
		String json = """
				{
				  "eventId": "%s",
				  "eventType": "BetSettled",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "tenantId": "acme",
				  "userId": "%s",
				  "payload": {
				    "betId": "%s",
				    "bettingHouseId": "%s",
				    "bettingHouseName": "House",
				    "sportId": "%s",
				    "sportName": "Sport",
				    "leagueId": "%s",
				    "leagueName": "League",
				    "marketId": "%s",
				    "marketName": "Market",
				    "tipsterId": null,
				    "tipsterName": null,
				    "team1Id": "%s",
				    "team1Name": "Real Madrid",
				    "team2Id": "%s",
				    "team2Name": "Barcelona",
				    "stake": 100.0,
				    "odd": 1.5,
				    "status": "won",
				    "profit": 50.0,
				    "settledAt": "%s"
				  }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), Instant.now());
		return objectMapper.readTree(json);
	}

	@Test
	void shouldAcceptAValidBetCreatedEvent() {
		assertThatNoException().isThrownBy(() -> validator.validate("BetCreated", validBetCreated()));
	}

	@Test
	void shouldAcceptAValidBetSettledEventWithTeamDimensions() {
		assertThatNoException().isThrownBy(() -> validator.validate("BetSettled", validBetSettled()));
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
	void shouldRejectABetCreatedEventWithAnUnknownField() throws Exception {
		JsonNode event = validBetCreated();
		((ObjectNode) event.get("payload")).put("unexpectedField", "x");

		assertThatThrownBy(() -> validator.validate("BetCreated", event))
				.isInstanceOf(InvalidBetEventException.class);
	}

	@Test
	void shouldRejectAnUnknownEventType() throws Exception {
		JsonNode node = objectMapper.readTree("{}");

		assertThatThrownBy(() -> validator.validate("SomethingElse", node))
				.isInstanceOf(UnknownBetEventTypeException.class);
	}
}
