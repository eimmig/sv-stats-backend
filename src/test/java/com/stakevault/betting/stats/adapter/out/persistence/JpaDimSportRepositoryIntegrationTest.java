package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimSportRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimSportRepository dimSportRepository;

	JpaDimSportRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			DimSportRepository dimSportRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimSportRepository = dimSportRepository;
	}

	@Test
	void shouldSaveAndPersistTheRow() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			DimSport saved = dimSportRepository.save(new DimSport(id, "Futebol"));

			assertThat(saved.name()).isEqualTo("Futebol");
			assertThat(namesOf(id)).containsExactly("Futebol");
		}
	}

	@Test
	void shouldInsertIfAbsentOnlyOnce() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			dimSportRepository.insertIfAbsent(new DimSport(id, "Futebol"));
			dimSportRepository.insertIfAbsent(new DimSport(id, "Other"));

			assertThat(namesOf(id)).containsExactly("Futebol");
		}
	}

	private List<String> namesOf(UUID id) {
		return jdbcTemplate.queryForList("SELECT name FROM \"" + schema.value() + "\".dim_sport WHERE id = ?",
				String.class, id);
	}
}
