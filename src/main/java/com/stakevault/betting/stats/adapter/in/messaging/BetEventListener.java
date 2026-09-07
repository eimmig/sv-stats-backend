package com.stakevault.betting.stats.adapter.in.messaging;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.model.TenantSchemaNotFoundException;
import com.stakevault.betting.stats.domain.port.in.BetCreatedEvent;
import com.stakevault.betting.stats.domain.port.in.BetSettledEvent;
import com.stakevault.betting.stats.domain.port.in.ProcessBetEventUseCase;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;

@Component
public class BetEventListener {

	private static final Logger log = LoggerFactory.getLogger(BetEventListener.class);

	private final BetEventSchemaValidator schemaValidator;
	private final ProvisionTenantSchemaUseCase provisionTenantSchema;
	private final ProcessBetEventUseCase processBetEvent;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public BetEventListener(BetEventSchemaValidator schemaValidator, ProvisionTenantSchemaUseCase provisionTenantSchema,
			ProcessBetEventUseCase processBetEvent) {
		this.schemaValidator = schemaValidator;
		this.provisionTenantSchema = provisionTenantSchema;
		this.processBetEvent = processBetEvent;
	}

	@RabbitListener(queues = "stats.bet-events")
	public void onMessage(byte[] body) throws IOException {
		JsonNode event = objectMapper.readTree(body);
		String eventType = event.path("eventType").asText();
		schemaValidator.validate(eventType, event);

		UUID eventId = UUID.fromString(event.path("eventId").asText());
		String tenantSlug = event.path("tenantId").asText();
		TenantSchemaName schema;
		try {
			schema = TenantSchemaName.fromSlug(tenantSlug);
			provisionTenantSchema.migrateIfPending(tenantSlug);
		} catch (IllegalArgumentException | TenantSchemaNotFoundException cause) {
			throw new AmqpRejectAndDontRequeueException(
					"cannot process " + eventType + " for unresolvable tenant '" + tenantSlug + "'", cause);
		}

		try (var _ = TenantContextScope.open(schema)) {
			JsonNode payload = event.path("payload");
			if ("BetCreated".equals(eventType)) {
				processBetEvent.processCreated(eventId, toBetCreatedEvent(payload));
			} else {
				processBetEvent.processSettled(eventId, toBetSettledEvent(payload));
			}
		}

		if (log.isInfoEnabled()) {
			log.info("processed {} event {} for tenant {}", eventType, eventId, tenantSlug);
		}
	}

	private static BetCreatedEvent toBetCreatedEvent(JsonNode payload) {
		return new BetCreatedEvent(uuid(payload, "betId"), uuid(payload, "bettingHouseId"),
				payload.path("bettingHouseName").asText(), uuid(payload, "sportId"), payload.path("sportName").asText(),
				uuid(payload, "leagueId"), payload.path("leagueName").asText(), uuid(payload, "marketId"),
				payload.path("marketName").asText(), nullableUuid(payload, "tipsterId"),
				nullableText(payload, "tipsterName"), decimal(payload, "stake"), instant(payload, "betDate"));
	}

	private static BetSettledEvent toBetSettledEvent(JsonNode payload) {
		return new BetSettledEvent(uuid(payload, "betId"), uuid(payload, "bettingHouseId"),
				payload.path("bettingHouseName").asText(), uuid(payload, "sportId"), payload.path("sportName").asText(),
				uuid(payload, "leagueId"), payload.path("leagueName").asText(), uuid(payload, "marketId"),
				payload.path("marketName").asText(), nullableUuid(payload, "tipsterId"),
				nullableText(payload, "tipsterName"), decimal(payload, "stake"),
				BetStatus.valueOf(payload.path("status").asText().toUpperCase()), decimal(payload, "profit"),
				instant(payload, "settledAt"));
	}

	private static UUID uuid(JsonNode payload, String field) {
		return UUID.fromString(payload.path(field).asText());
	}

	private static UUID nullableUuid(JsonNode payload, String field) {
		JsonNode node = payload.path(field);
		return node.isNull() || node.isMissingNode() ? null : UUID.fromString(node.asText());
	}

	private static String nullableText(JsonNode payload, String field) {
		JsonNode node = payload.path(field);
		return node.isNull() || node.isMissingNode() ? null : node.asText();
	}

	private static BigDecimal decimal(JsonNode payload, String field) {
		return payload.path(field).decimalValue();
	}

	private static Instant instant(JsonNode payload, String field) {
		return Instant.parse(payload.path(field).asText());
	}
}
