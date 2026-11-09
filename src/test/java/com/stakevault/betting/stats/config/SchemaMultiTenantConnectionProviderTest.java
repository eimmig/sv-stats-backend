package com.stakevault.betting.stats.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SchemaMultiTenantConnectionProviderTest {

	private final DataSource dataSource = mock(DataSource.class);
	private final Connection pooled = mock(Connection.class);
	private final Connection physical = mock(Connection.class);
	private SchemaMultiTenantConnectionProvider provider;

	@BeforeEach
	void setUp() throws SQLException {
		when(dataSource.getConnection()).thenReturn(pooled);
		when(pooled.isWrapperFor(Connection.class)).thenReturn(true);
		when(pooled.unwrap(Connection.class)).thenReturn(physical);
		provider = new SchemaMultiTenantConnectionProvider(dataSource);
	}

	@Test
	void shouldSetTheSchemaOnlyWhenThePhysicalConnectionHasAnotherOne() throws SQLException {
		assertThat(provider.getConnection("tenant_a")).isSameAs(pooled);
		provider.releaseConnection("tenant_a", pooled);
		provider.getConnection("tenant_a");
		provider.releaseConnection("tenant_a", pooled);

		verify(physical, times(1)).setSchema("tenant_a");
		verify(pooled, never()).setSchema("public");

		provider.getConnection("tenant_b");
		provider.getAnyConnection();

		verify(physical).setSchema("tenant_b");
		verify(physical).setSchema("public");
		verify(pooled, times(2)).close();
	}

	@Test
	void shouldUseTheConnectionItselfWhenItWrapsNothing() throws SQLException {
		Connection plain = mock(Connection.class);
		when(dataSource.getConnection()).thenReturn(plain);

		provider.getConnection("tenant_a");

		verify(plain).setSchema("tenant_a");
	}

	@Test
	void shouldCloseTheConnectionAndForgetItsSchemaWhenTheSetFails() throws SQLException {
		doThrow(new SQLException("boom")).doNothing().when(physical).setSchema("tenant_a");

		assertThatThrownBy(() -> provider.getConnection("tenant_a")).isInstanceOf(SQLException.class);
		verify(pooled).close();

		provider.getConnection("tenant_a");
		verify(physical, times(2)).setSchema("tenant_a");
	}

	@Test
	void shouldUnwrapOnlyItself() {
		assertThat(provider.isUnwrappableAs(SchemaMultiTenantConnectionProvider.class)).isTrue();
		assertThat(provider.unwrap(SchemaMultiTenantConnectionProvider.class)).isSameAs(provider);
		assertThat(provider.unwrap(String.class)).isNull();
		assertThat(provider.supportsAggressiveRelease()).isFalse();
	}
}
