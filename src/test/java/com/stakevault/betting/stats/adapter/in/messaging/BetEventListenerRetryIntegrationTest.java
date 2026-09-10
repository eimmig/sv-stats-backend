package com.stakevault.betting.stats.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.domain.port.in.BetCreatedEvent;
import com.stakevault.betting.stats.domain.port.in.BetSettledEvent;
import com.stakevault.betting.stats.domain.port.in.ProcessBetEventUseCase;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

// Contexto proprio (bean @Primary abaixo substitui ProcessBetEventUseCase) - nao reaproveita o
// contexto cacheado de BetEventListenerIntegrationTest, propositalmente isolado.
class BetEventListenerRetryIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final String EXCHANGE = "bets.events";
	private static final String QUEUE = "stats.bet-events";
	private static final String DLQ = "stats.bet-events.dlq";

	private final RabbitTemplate rabbitTemplate;
	private final RabbitAdmin rabbitAdmin;

	BetEventListenerRetryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
		super(provisionTenantSchema, jdbcTemplate);
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitAdmin = rabbitAdmin;
	}

	// RabbitMQ 4.3+ nao conta nack(requeue=true) para x-delivery-limit - simula a falha
	// transitoria que so o retry de aplicacao (spring.rabbitmq.listener.simple.retry, ver
	// application.yml) consegue esgotar e converter em reject explicito (sempre morta-letra).
	@TestConfiguration
	static class AlwaysFailingProcessorConfig {

		@Bean
		@Primary
		ProcessBetEventUseCase alwaysFailingProcessBetEvent() {
			return new ProcessBetEventUseCase() {
				@Override
				public void processCreated(UUID eventId, BetCreatedEvent event) {
					throw new IllegalStateException("simulated transient failure");
				}

				@Override
				public void processSettled(UUID eventId, BetSettledEvent event) {
					throw new IllegalStateException("simulated transient failure");
				}
			};
		}
	}

	private void publish(String routingKey, String body) {
		Message message = MessageBuilder.withBody(body.getBytes(StandardCharsets.UTF_8))
				.setContentType("application/json")
				.build();
		rabbitTemplate.send(EXCHANGE, routingKey, message);
	}

	private long queueMessageCount(String queue) {
		var properties = rabbitAdmin.getQueueProperties(queue);
		return properties == null ? -1 : ((Number) properties.get(RabbitAdmin.QUEUE_MESSAGE_COUNT)).longValue();
	}

	private String betCreatedBody(String eventId, UUID betId, String tenantSlug) {
		return """
				{
				  "eventId": "%s",
				  "eventType": "BetCreated",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "tenantId": "%s",
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
				""".formatted(eventId, Instant.now(), tenantSlug, UUID.randomUUID(), betId, UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());
	}

	@Test
	void shouldDeadLetterAfterExhaustingApplicationRetries() {
		UUID betId = UUID.randomUUID();
		String eventId = UUID.randomUUID().toString();

		publish("bet.created", betCreatedBody(eventId, betId, tenantSlug));

		Message deadLettered = rabbitTemplate.receive(DLQ, 15000);

		assertThat(deadLettered).isNotNull();
		assertThat(queueMessageCount(QUEUE)).isZero();
	}
}
