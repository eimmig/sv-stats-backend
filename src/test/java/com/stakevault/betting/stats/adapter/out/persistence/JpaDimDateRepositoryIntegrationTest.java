package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimDateRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimDateRepository dimDateRepository;

	JpaDimDateRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			DimDateRepository dimDateRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimDateRepository = dimDateRepository;
	}

	@Test
	void shouldSaveAndFindByNaturalKey() {
		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimDateRepository.findByDayAndMonthAndYear(6, 9, 2026)).isEmpty();

			dimDateRepository.save(new DimDate(UUID.randomUUID(), 6, 9, 2026, 3, "SUNDAY"));

			DimDate found = dimDateRepository.findByDayAndMonthAndYear(6, 9, 2026).orElseThrow();
			assertThat(found.quarter()).isEqualTo(3);
			assertThat(found.dayOfWeek()).isEqualTo("SUNDAY");
		}
	}
}
