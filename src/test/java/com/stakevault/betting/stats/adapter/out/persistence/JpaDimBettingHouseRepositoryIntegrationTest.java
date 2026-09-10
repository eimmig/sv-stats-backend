package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimBettingHouseRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimBettingHouseRepository dimBettingHouseRepository;

	JpaDimBettingHouseRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, DimBettingHouseRepository dimBettingHouseRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimBettingHouseRepository = dimBettingHouseRepository;
	}

	@Test
	void shouldSaveAndReportExisting() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimBettingHouseRepository.existsById(id)).isFalse();

			DimBettingHouse saved = dimBettingHouseRepository.save(new DimBettingHouse(id, "Bet365"));

			assertThat(saved.name()).isEqualTo("Bet365");
			assertThat(dimBettingHouseRepository.existsById(id)).isTrue();
		}
	}
}
