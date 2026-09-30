package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.RepeatedTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class DimensionResolverConcurrencyIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final int CONSUMERS = 8;

	private final DimensionResolver resolver;
	private final TransactionTemplate transactionTemplate;

	DimensionResolverConcurrencyIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, DimensionResolver resolver, PlatformTransactionManager transactionManager) {
		super(provisionTenantSchema, jdbcTemplate);
		this.resolver = resolver;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
	}

	@RepeatedTest(5)
	void shouldResolveTheSameDimensionsFromConcurrentConsumersWithoutErrorsOrDuplicates() throws Exception {
		UUID sportId = UUID.randomUUID();
		UUID houseId = UUID.randomUUID();
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		CountDownLatch start = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(CONSUMERS);
		List<Future<List<UUID>>> futures = new ArrayList<>();
		try {
			for (int i = 0; i < CONSUMERS; i++) {
				futures.add(executor.submit(() -> {
					start.await();
					try (var _ = TenantContextScope.open(schema)) {
						return transactionTemplate.execute(status -> {
							resolver.resolveBettingHouse(houseId, "House");
							resolver.resolveSport(sportId, "Football");
							UUID dateId = resolver.resolveDate(instant);
							UUID teamId = resolver.resolveTeam(null, "Flamengo", sportId);
							return List.of(dateId, teamId);
						});
					}
				}));
			}
			start.countDown();

			Set<UUID> dateIds = new HashSet<>();
			Set<UUID> teamIds = new HashSet<>();
			for (Future<List<UUID>> future : futures) {
				List<UUID> resolved = future.get();
				dateIds.add(resolved.get(0));
				teamIds.add(resolved.get(1));
			}

			assertThat(dateIds).hasSize(1);
			assertThat(teamIds).hasSize(1);
			assertThat(count("dim_date")).isEqualTo(1);
			assertThat(count("dim_team")).isEqualTo(1);
			assertThat(count("dim_sport")).isEqualTo(1);
			assertThat(count("dim_betting_house")).isEqualTo(1);
		} finally {
			executor.shutdownNow();
		}
	}

	private int count(String table) {
		Integer total = jdbcTemplate.queryForObject("SELECT count(*) FROM \"" + schema.value() + "\"." + table,
				Integer.class);
		return total == null ? 0 : total;
	}
}
