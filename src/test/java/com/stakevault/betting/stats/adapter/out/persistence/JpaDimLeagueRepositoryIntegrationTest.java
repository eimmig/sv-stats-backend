package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimLeagueRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimLeagueRepository dimLeagueRepository;

	JpaDimLeagueRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, DimLeagueRepository dimLeagueRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimLeagueRepository = dimLeagueRepository;
	}

	@Test
	void shouldSaveAndPersistTheRow() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			DimLeague saved = dimLeagueRepository.save(new DimLeague(id, "Serie A"));

			assertThat(saved.name()).isEqualTo("Serie A");
			assertThat(namesOf(id)).containsExactly("Serie A");
		}
	}

	@Test
	void shouldInsertIfAbsentOnlyOnce() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			dimLeagueRepository.insertIfAbsent(new DimLeague(id, "Serie A"));
			dimLeagueRepository.insertIfAbsent(new DimLeague(id, "Other"));

			assertThat(namesOf(id)).containsExactly("Serie A");
		}
	}

	private List<String> namesOf(UUID id) {
		return jdbcTemplate.queryForList("SELECT name FROM \"" + schema.value() + "\".dim_league WHERE id = ?",
				String.class, id);
	}
}
