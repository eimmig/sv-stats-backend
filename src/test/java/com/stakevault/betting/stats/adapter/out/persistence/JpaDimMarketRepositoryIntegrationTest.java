package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimMarketRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimMarketRepository dimMarketRepository;

	JpaDimMarketRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, DimMarketRepository dimMarketRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimMarketRepository = dimMarketRepository;
	}

	@Test
	void shouldSaveAndPersistTheRow() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			DimMarket saved = dimMarketRepository.save(new DimMarket(id, "Moneyline"));

			assertThat(saved.name()).isEqualTo("Moneyline");
			assertThat(namesOf(id)).containsExactly("Moneyline");
		}
	}

	@Test
	void shouldInsertIfAbsentOnlyOnce() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			dimMarketRepository.insertIfAbsent(new DimMarket(id, "Moneyline"));
			dimMarketRepository.insertIfAbsent(new DimMarket(id, "Other"));

			assertThat(namesOf(id)).containsExactly("Moneyline");
		}
	}

	private List<String> namesOf(UUID id) {
		return jdbcTemplate.queryForList("SELECT name FROM \"" + schema.value() + "\".dim_market WHERE id = ?",
				String.class, id);
	}
}
