package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;

@Component
public class JdbcFlywayTenantSchemaGateway implements TenantSchemaGateway {

	private static final String MIGRATION_LOCATION = "classpath:db/migration";

	private final JdbcTemplate jdbcTemplate;
	private final DataSource dataSource;
	private final Set<String> migratedSchemas = ConcurrentHashMap.newKeySet();

	public JdbcFlywayTenantSchemaGateway(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public boolean exists(TenantSchemaName schema) {
		Integer count = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.schemata WHERE schema_name = ?",
				Integer.class, schema.value());
		return count != null && count > 0;
	}

	@Override
	public void createAndMigrate(TenantSchemaName schema) {
		flywayFor(schema, true).migrate();
		migratedSchemas.add(schema.value());
	}

	@Override
	public void migrateExistingOnly(TenantSchemaName schema) {
		if (migratedSchemas.contains(schema.value())) {
			return;
		}
		flywayFor(schema, false).migrate();
		migratedSchemas.add(schema.value());
	}

	private Flyway flywayFor(TenantSchemaName schema, boolean createSchemas) {
		FluentConfiguration config = Flyway.configure()
				.dataSource(dataSource)
				.schemas(schema.value())
				.createSchemas(createSchemas)
				.locations(MIGRATION_LOCATION);
		return config.load();
	}
}
