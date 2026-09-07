package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
	void shouldSaveAndReportExisting() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimSportRepository.existsById(id)).isFalse();

			DimSport saved = dimSportRepository.save(new DimSport(id, "Futebol"));

			assertThat(saved.name()).isEqualTo("Futebol");
			assertThat(dimSportRepository.existsById(id)).isTrue();
		}
	}
}
