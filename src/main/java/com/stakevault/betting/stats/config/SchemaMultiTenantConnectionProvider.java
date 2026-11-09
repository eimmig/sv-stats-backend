package com.stakevault.betting.stats.config;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import javax.sql.DataSource;

import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.stereotype.Component;

/**
 * Entrega ao Hibernate uma conexao com o search_path do schema da organizacao (ou de public, sem
 * organizacao). Lembra o schema de cada conexao fisica do pool e so executa SET quando ele muda: antes,
 * cada emprestimo fazia SET para a organizacao e outro SET para public na devolucao, e a medicao de
 * 2026-11-09 contou 394 mil SET para 20 mil apostas, quase metade das instrucoes do banco (feat-038).
 *
 * <p>O SET vai direto na conexao fisica, sem passar pelo proxy do Hikari, para o pool nao restaurar o
 * schema na devolucao. Todo uso do Hibernate passa por getConnection ou getAnyConnection, que ajustam
 * o schema antes de entregar. Fora do Hibernate, so o gateway do Flyway usa o DataSource, com consultas
 * qualificadas pelo schema, e o Flyway restaura o search_path que encontrou.
 */
@Component
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

	private static final String PUBLIC_SCHEMA = "public";

	private final transient DataSource dataSource;
	private final transient Map<Connection, String> schemaByConnection = Collections
			.synchronizedMap(new WeakHashMap<>());

	public SchemaMultiTenantConnectionProvider(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public Connection getAnyConnection() throws SQLException {
		return withSchema(PUBLIC_SCHEMA);
	}

	@Override
	public void releaseAnyConnection(Connection connection) throws SQLException {
		connection.close();
	}

	@Override
	public Connection getConnection(String tenantIdentifier) throws SQLException {
		return withSchema(tenantIdentifier);
	}

	@Override
	public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
		releaseAnyConnection(connection);
	}

	private Connection withSchema(String schema) throws SQLException {
		Connection connection = dataSource.getConnection();
		try {
			Connection physical = physical(connection);
			if (!Objects.equals(schemaByConnection.get(physical), schema)) {
				schemaByConnection.remove(physical);
				physical.setSchema(schema);
				schemaByConnection.put(physical, schema);
			}
			return connection;
		} catch (SQLException e) {
			releaseAnyConnection(connection);
			throw e;
		}
	}

	private static Connection physical(Connection connection) throws SQLException {
		Connection unwrapped = connection.isWrapperFor(Connection.class) ? connection.unwrap(Connection.class) : null;
		return unwrapped != null ? unwrapped : connection;
	}

	@Override
	public boolean supportsAggressiveRelease() {
		return false;
	}

	@Override
	public boolean isUnwrappableAs(Class<?> unwrapType) {
		return unwrapType.isInstance(this);
	}

	@Override
	public <T> T unwrap(Class<T> unwrapType) {
		return unwrapType.isInstance(this) ? unwrapType.cast(this) : null;
	}
}
