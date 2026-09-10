package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimTeamRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimTeamRepository dimTeamRepository;

	JpaDimTeamRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			DimTeamRepository dimTeamRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimTeamRepository = dimTeamRepository;
	}

	@Test
	void shouldSaveAndFindByNaturalKey() {
		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimTeamRepository.findByName("Flamengo")).isEmpty();

			UUID id = UUID.randomUUID();
			dimTeamRepository.save(new DimTeam(id, "Flamengo"));

			DimTeam found = dimTeamRepository.findByName("Flamengo").orElseThrow();
			assertThat(found.id()).isEqualTo(id);
		}
	}
}
