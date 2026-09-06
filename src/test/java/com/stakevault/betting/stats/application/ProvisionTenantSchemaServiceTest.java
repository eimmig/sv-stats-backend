package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.model.TenantSchemaNotFoundException;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;

@ExtendWith(MockitoExtension.class)
class ProvisionTenantSchemaServiceTest {

	private static final TenantSchemaName SCHEMA = new TenantSchemaName("tenant_acme");

	@Mock
	private TenantSchemaGateway gateway;

	private ProvisionTenantSchemaService service;

	@BeforeEach
	void setUp() {
		service = new ProvisionTenantSchemaService(gateway);
	}

	@Test
	void shouldReportExistsFromGateway() {
		when(gateway.exists(SCHEMA)).thenReturn(true);

		assertThat(service.exists("acme")).isTrue();
	}

	@Test
	void shouldCreateAndMigrateWhenEnsuringSchemaExists() {
		service.ensureSchemaExists("acme");

		verify(gateway).createAndMigrate(SCHEMA);
	}

	@Test
	void shouldMigrateExistingOnlyWhenSchemaAlreadyExists() {
		when(gateway.exists(SCHEMA)).thenReturn(true);

		service.migrateIfPending("acme");

		verify(gateway).migrateExistingOnly(SCHEMA);
		verify(gateway, never()).createAndMigrate(SCHEMA);
	}

	@Test
	void shouldThrowWithoutMigratingWhenSchemaDoesNotExist() {
		when(gateway.exists(SCHEMA)).thenReturn(false);

		assertThatThrownBy(() -> service.migrateIfPending("acme"))
				.isInstanceOf(TenantSchemaNotFoundException.class);

		verify(gateway, never()).migrateExistingOnly(SCHEMA);
		verify(gateway, never()).createAndMigrate(SCHEMA);
	}
}
