package com.stakevault.betting.stats.adapter.in.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class BetEventListenerIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final String EXCHANGE = "bets.events";
	private static final String QUEUE = "stats.bet-events";
	private static final String DLQ = "stats.bet-events.dlq";

	private final RabbitTemplate rabbitTemplate;
	private final RabbitAdmin rabbitAdmin;
	private final FactBetRepository factBetRepository;
	private final ProcessedEventRepository processedEventRepository;

	BetEventListenerIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			RabbitTemplate rabbitTemplate, RabbitAdmin rabbitAdmin, FactBetRepository factBetRepository,
			ProcessedEventRepository processedEventRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.rabbitTemplate = rabbitTemplate;
		this.rabbitAdmin = rabbitAdmin;
		this.factBetRepository = factBetRepository;
		this.processedEventRepository = processedEventRepository;
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
		return betCreatedBody(eventId, betId, tenantSlug, UUID.randomUUID(), null, null);
	}

	private String betCreatedBody(String eventId, UUID betId, String tenantSlug, UUID sportId, UUID team1Id,
			String team1) {
		String team1IdJson = team1Id == null ? "null" : "\"" + team1Id + "\"";
		String team1Json = team1 == null ? "null" : "\"" + team1 + "\"";
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
				    "team1Id": %s,
				    "team1": %s,
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
				""".formatted(eventId, Instant.now(), tenantSlug, UUID.randomUUID(), betId, UUID.randomUUID(), sportId,
				UUID.randomUUID(), UUID.randomUUID(), team1IdJson, team1Json, Instant.now());
	}

	private String betSettledBody(String eventId, UUID betId, String tenantSlug) {
		return betSettledBody(eventId, betId, tenantSlug, UUID.randomUUID(), null, null);
	}

	private String betSettledBody(String eventId, UUID betId, String tenantSlug, UUID sportId, UUID team1Id,
			String team1Name) {
		String team1IdJson = team1Id == null ? "null" : "\"" + team1Id + "\"";
		String team1NameJson = team1Name == null ? "null" : "\"" + team1Name + "\"";
		return """
				{
				  "eventId": "%s",
				  "eventType": "BetSettled",
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
				    "team1Id": %s,
				    "team1Name": %s,
				    "team2Id": null,
				    "team2Name": null,
				    "stake": 100.0,
				    "odd": 1.5,
				    "status": "won",
				    "profit": 50.0,
				    "settledAt": "%s"
				  }
				}
				""".formatted(eventId, Instant.now(), tenantSlug, UUID.randomUUID(), betId, UUID.randomUUID(), sportId,
				UUID.randomUUID(), UUID.randomUUID(), team1IdJson, team1NameJson, Instant.now());
	}

	@Test
	void shouldInsertFactBetPendingOnBetCreated() {
		UUID betId = UUID.randomUUID();
		String eventId = UUID.randomUUID().toString();

		publish("bet.created", betCreatedBody(eventId, betId, tenantSlug));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(betId).orElseThrow();
				assertThat(factBet.status()).isEqualTo(BetStatus.PENDING);
				assertThat(factBet.profit()).isNull();
				assertThat(processedEventRepository.existsByEventId(UUID.fromString(eventId))).isTrue();
			}
		});
		assertThat(queueMessageCount(QUEUE)).isZero();
		assertThat(queueMessageCount(DLQ)).isZero();
	}

	@Test
	void shouldUpsertFactBetOnBetSettled() {
		UUID betId = UUID.randomUUID();
		publish("bet.created", betCreatedBody(UUID.randomUUID().toString(), betId, tenantSlug));
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(betId)).isPresent();
			}
		});

		publish("bet.settled", betSettledBody(UUID.randomUUID().toString(), betId, tenantSlug));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(betId).orElseThrow();
				assertThat(factBet.status()).isEqualTo(BetStatus.WON);
				assertThat(factBet.profit()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
				assertThat(factBet.isWin()).isTrue();
			}
		});
		Integer rowCount;
		try (var _ = TenantContextScope.open(schema)) {
			rowCount = jdbcTemplate.queryForObject(
					"SELECT count(*) FROM \"" + schema.value() + "\".fact_bet WHERE id = ?", Integer.class, betId);
		}
		assertThat(rowCount).isEqualTo(1);
	}

	@Test
	void shouldHandleBetSettledArrivingBeforeBetCreated() {
		UUID betId = UUID.randomUUID();

		publish("bet.settled", betSettledBody(UUID.randomUUID().toString(), betId, tenantSlug));
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(betId)).isPresent();
			}
		});

		publish("bet.created", betCreatedBody(UUID.randomUUID().toString(), betId, tenantSlug));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(queueMessageCount(QUEUE)).isZero());
		try (var _ = TenantContextScope.open(schema)) {
			// BetCreated chegando depois nao deve reverter a liquidacao ja aplicada.
			FactBet factBet = factBetRepository.findById(betId).orElseThrow();
			assertThat(factBet.status()).isEqualTo(BetStatus.WON);
			assertThat(factBet.profit()).isEqualByComparingTo(BigDecimal.valueOf(50.0));
		}
	}

	@Test
	void shouldNotReprocessARedeliveredEventId() {
		UUID betId = UUID.randomUUID();
		String eventId = UUID.randomUUID().toString();
		publish("bet.created", betCreatedBody(eventId, betId, tenantSlug));
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(betId)).isPresent();
			}
		});

		// Mesmo eventId, betDate diferente - se fosse reprocessado, sobrescreveria o dateId.
		publish("bet.created", betCreatedBody(eventId, betId, tenantSlug));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> assertThat(queueMessageCount(QUEUE)).isZero());
		Integer processedCount;
		try (var _ = TenantContextScope.open(schema)) {
			processedCount = jdbcTemplate.queryForObject(
					"SELECT count(*) FROM \"" + schema.value() + "\".processed_event WHERE event_id = ?",
					Integer.class, UUID.fromString(eventId));
		}
		assertThat(processedCount).isEqualTo(1);
	}

	@Test
	void shouldResolveTeamDimensionUsingTheCatalogIdFromTheEvent() {
		UUID betId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		UUID catalogTeamId = UUID.randomUUID();

		publish("bet.created",
				betCreatedBody(UUID.randomUUID().toString(), betId, tenantSlug, sportId, catalogTeamId, "Flamengo"));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(betId).orElseThrow();
				assertThat(factBet.team1Id()).isEqualTo(catalogTeamId);
			}
		});
	}

	// Cenario central do feat-018: um time ja resolvido por nome antes do catalogo TEAM de
	// bets-service existir (id local, sem catalogId no evento) tem que continuar respondendo
	// pelo mesmo id quando reaparece com o id real do catalogo - senao o segundo BetCreated
	// violaria UNIQUE(name, sport_id) e cairia na DLQ sem se recuperar sozinho.
	@Test
	void shouldKeepTheFirstResolvedTeamIdWhenTheSameNameReappearsWithADifferentCatalogId() {
		UUID firstBetId = UUID.randomUUID();
		UUID secondBetId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		UUID legacyLocalTeamId = UUID.randomUUID();
		UUID laterCatalogTeamId = UUID.randomUUID();

		publish("bet.created", betCreatedBody(UUID.randomUUID().toString(), firstBetId, tenantSlug, sportId,
				legacyLocalTeamId, "Flamengo"));
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(firstBetId)).isPresent();
			}
		});

		publish("bet.created", betCreatedBody(UUID.randomUUID().toString(), secondBetId, tenantSlug, sportId,
				laterCatalogTeamId, "Flamengo"));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(secondBetId).orElseThrow();
				assertThat(factBet.team1Id()).isEqualTo(legacyLocalTeamId);
			}
		});
		assertThat(queueMessageCount(QUEUE)).isZero();
		assertThat(queueMessageCount(DLQ)).isZero();
	}

	@Test
	void shouldResolveTeamDimensionWhenBetSettledArrivesBeforeBetCreated() {
		UUID betId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		UUID catalogTeamId = UUID.randomUUID();

		publish("bet.settled", betSettledBody(UUID.randomUUID().toString(), betId, tenantSlug, sportId,
				catalogTeamId, "Flamengo"));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(betId).orElseThrow();
				assertThat(factBet.team1Id()).isEqualTo(catalogTeamId);
			}
		});
	}

	// feat-018.3: BetSettled sem team1Id/team1Name (bet que ja tem o time resolvido pelo insert
	// de BetCreated) nao deve apagar o que ja foi gravado.
	@Test
	void shouldPreserveTeamDimensionWhenBetSettledDoesNotCarryIt() {
		UUID betId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		UUID catalogTeamId = UUID.randomUUID();

		publish("bet.created",
				betCreatedBody(UUID.randomUUID().toString(), betId, tenantSlug, sportId, catalogTeamId, "Flamengo"));
		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(betId)).isPresent();
			}
		});

		publish("bet.settled", betSettledBody(UUID.randomUUID().toString(), betId, tenantSlug));

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				FactBet factBet = factBetRepository.findById(betId).orElseThrow();
				assertThat(factBet.status()).isEqualTo(BetStatus.WON);
				assertThat(factBet.team1Id()).isEqualTo(catalogTeamId);
			}
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
				  "tenantId": "%s",
				  "userId": "%s",
				  "payload": {
				    "betId": "not-a-uuid"
				  }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), tenantSlug, UUID.randomUUID());

		publish("bet.created", body);

		Message deadLettered = rabbitTemplate.receive(DLQ, 10000);

		assertThat(deadLettered).isNotNull();
		assertThat(queueMessageCount(QUEUE)).isZero();
	}

	@Test
	void shouldDeadLetterAMessageForAnUnprovisionedTenant() {
		String body = betCreatedBody(UUID.randomUUID().toString(), UUID.randomUUID(), "no-such-tenant");

		publish("bet.created", body);

		Message deadLettered = rabbitTemplate.receive(DLQ, 10000);

		assertThat(deadLettered).isNotNull();
		assertThat(queueMessageCount(QUEUE)).isZero();
	}
}
