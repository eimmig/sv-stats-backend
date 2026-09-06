package com.stakevault.betting.stats.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.stakevault.betting.stats.TestcontainersConfiguration;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class BetEventListenerIntegrationTest {

	private static final String EXCHANGE = "bets.events";
	private static final String QUEUE = "stats.bet-events";
	private static final String DLQ = "stats.bet-events.dlq";

	private final RabbitTemplate rabbitTemplate;
	private final RabbitAdmin rabbitAdmin;

	BetEventListenerIntegrationTest(RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin) {
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitAdmin = rabbitAdmin;
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

	@Test
	void shouldConsumeAValidBetCreatedEventWithoutError() {
		String eventId = UUID.randomUUID().toString();
		String body = """
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
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

		publish("bet.created", body);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			assertThat(queueMessageCount(QUEUE)).isZero();
			assertThat(queueMessageCount(DLQ)).isZero();
		});
	}

	@Test
	void shouldConsumeAValidBetSettledEventWithoutError() {
		String eventId = UUID.randomUUID().toString();
		String body = """
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
				    "sportId": "%s",
				    "leagueId": "%s",
				    "marketId": "%s",
				    "tipsterId": null,
				    "stake": 100.0,
				    "odd": 1.5,
				    "status": "won",
				    "profit": 50.0,
				    "settledAt": "%s"
				  }
				}
				""".formatted(eventId, Instant.now(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), Instant.now());

		publish("bet.settled", body);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			assertThat(queueMessageCount(QUEUE)).isZero();
			assertThat(queueMessageCount(DLQ)).isZero();
		});
	}

	@Test
	void shouldDeadLetterAMessageThatViolatesTheSchema() {
		String body = """
				{
				  "eventId": "%s",
				  "eventType": "BetCreated",
				  "schemaVersion": 1,
				  "occurredAt": "%s",
				  "tenantId": "acme",
				  "userId": "%s",
				  "payload": {
				    "betId": "not-a-uuid"
				  }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), UUID.randomUUID());

		publish("bet.created", body);

		Message deadLettered = rabbitTemplate.receive(DLQ, 10000);

		assertThat(deadLettered).isNotNull();
		assertThat(queueMessageCount(QUEUE)).isZero();
	}
}
