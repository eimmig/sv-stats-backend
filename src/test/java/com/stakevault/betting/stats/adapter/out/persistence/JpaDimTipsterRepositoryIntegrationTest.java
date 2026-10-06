package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
	void shouldSaveAndPersistTheRow() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			DimTipster saved = dimTipsterRepository.save(new DimTipster(id, "Tipster1"));

			assertThat(saved.name()).isEqualTo("Tipster1");
			assertThat(namesOf(id)).containsExactly("Tipster1");
		}
	}

	@Test
	void shouldInsertIfAbsentOnlyOnce() {
		UUID id = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			dimTipsterRepository.insertIfAbsent(new DimTipster(id, "Tipster1"));
			dimTipsterRepository.insertIfAbsent(new DimTipster(id, "Other"));

			assertThat(namesOf(id)).containsExactly("Tipster1");
		}
	}

	private List<String> namesOf(UUID id) {
		return jdbcTemplate.queryForList("SELECT name FROM \"" + schema.value() + "\".dim_tipster WHERE id = ?",
				String.class, id);
	}
}
