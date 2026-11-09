package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;

class KnownDimensionsTest {

	private static final TenantSchemaName ORG_A = TenantSchemaName.fromSlug("org-a");
	private static final TenantSchemaName ORG_B = TenantSchemaName.fromSlug("org-b");

	private final KnownDimensions known = new KnownDimensions();

	@AfterEach
	void clearSynchronization() {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.clearSynchronization();
		}
	}

	@Test
	void shouldRememberPerOrganization() {
		try (var _ = TenantContextScope.open(ORG_A)) {
			known.remember("sport", "1", Boolean.TRUE);
			assertThat(known.contains("sport", "1")).isTrue();
			assertThat(known.contains("league", "1")).isFalse();
		}
		try (var _ = TenantContextScope.open(ORG_B)) {
			assertThat(known.contains("sport", "1")).isFalse();
		}
	}

	@Test
	void shouldIgnoreCallsWithoutOrganizationOrKey() {
		known.remember("sport", "1", Boolean.TRUE);
		assertThat(known.contains("sport", "1")).isFalse();
		try (var _ = TenantContextScope.open(ORG_A)) {
			known.remember("sport", null, Boolean.TRUE);
			known.remember("sport", "2", null);
			assertThat(known.contains("sport", "2")).isFalse();
		}
	}

	@Test
	void shouldRememberOnlyAfterTheTransactionCommits() {
		try (var _ = TenantContextScope.open(ORG_A)) {
			TransactionSynchronizationManager.initSynchronization();
			known.remember("market", "7", Boolean.TRUE);

			assertThat(known.contains("market", "7")).isFalse();

			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCommit();
			}
			assertThat(known.contains("market", "7")).isTrue();
		}
	}

	@Test
	void shouldForgetAWriteWhoseTransactionRolledBack() {
		try (var _ = TenantContextScope.open(ORG_A)) {
			TransactionSynchronizationManager.initSynchronization();
			known.remember("market", "8", Boolean.TRUE);
			for (TransactionSynchronization sync : TransactionSynchronizationManager.getSynchronizations()) {
				sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
			}
			assertThat(known.contains("market", "8")).isFalse();
		}
	}
}
