package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimTipsterRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimTipsterRepository dimTipsterRepository;

	JpaDimTipsterRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, DimTipsterRepository dimTipsterRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimTipsterRepository = dimTipsterRepository;
	}

	@Test
	void shouldSaveAndReportExisting() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			assertThat(dimTipsterRepository.existsById(id)).isFalse();

			DimTipster saved = dimTipsterRepository.save(new DimTipster(id, "Tipster1"));

			assertThat(saved.name()).isEqualTo("Tipster1");
			assertThat(dimTipsterRepository.existsById(id)).isTrue();
		}
	}
}
