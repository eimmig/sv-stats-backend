package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
	void shouldSaveAndReportExisting() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimLeagueRepository.existsById(id)).isFalse();

			DimLeague saved = dimLeagueRepository.save(new DimLeague(id, "Serie A"));

			assertThat(saved.name()).isEqualTo("Serie A");
			assertThat(dimLeagueRepository.existsById(id)).isTrue();
		}
	}
}
