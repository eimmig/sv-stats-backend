package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class DimDateStatisticsIntegrationTest extends TenantSchemaIntegrationSupport {

	DimDateStatisticsIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate) {
		super(provisionTenantSchema, jdbcTemplate);
	}

	@Test
	void tenantMigrationShouldCreateStatisticsOnTheAssembledDate() {
		String expression = jdbcTemplate.queryForObject("""
				SELECT pg_get_statisticsobjdef_expressions(s.oid)::text
				FROM pg_statistic_ext s JOIN pg_namespace n ON n.oid = s.stxnamespace
				WHERE n.nspname = ? AND s.stxname = 'dim_date_day'
				""", String.class, schema.value());

		assertThat(expression).isEqualTo("{\"make_date(year, month, day)\"}");
	}
}
