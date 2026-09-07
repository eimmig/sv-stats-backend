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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class CacheInvalidationOnEventIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final String EXCHANGE = "bets.events";

	private final RabbitTemplate rabbitTemplate;
	private final MetricsCacheRepository cacheRepository;

	CacheInvalidationOnEventIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, RabbitTemplate rabbitTemplate, MetricsCacheRepository cacheRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.rabbitTemplate = rabbitTemplate;
		this.cacheRepository = cacheRepository;
	}

	private BetMetrics staleCachedMetrics() {
		return new BetMetrics(BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(0.5),
				BigDecimal.valueOf(0.5), 1);
	}

	private String betSettledBody(UUID betId, Instant settledAt) {
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
				    "stake": 100.0,
				    "odd": 1.5,
				    "status": "won",
				    "profit": 50.0,
				    "settledAt": "%s"
				  }
				}
				""".formatted(UUID.randomUUID(), Instant.now(), tenantSlug, UUID.randomUUID(), betId, UUID.randomUUID(),
				UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), settledAt);
	}

	@Test
	void shouldEvictCachedDashboardWhenABetIsSettled() {
		Instant settledAt = Instant.now();
		try (var _ = TenantContextScope.open(schema)) {
			cacheRepository.saveOverall(staleCachedMetrics());
			assertThat(cacheRepository.findOverall()).isPresent();
		}

		Message message = MessageBuilder
				.withBody(betSettledBody(UUID.randomUUID(), settledAt).getBytes(StandardCharsets.UTF_8))
				.setContentType("application/json")
				.build();
		rabbitTemplate.send(EXCHANGE, "bet.settled", message);

		await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
			try (var _ = TenantContextScope.open(schema)) {
				assertThat(cacheRepository.findOverall()).isEmpty();
			}
		});
	}
}
