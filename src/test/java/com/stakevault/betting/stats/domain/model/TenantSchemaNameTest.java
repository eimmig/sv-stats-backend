package com.stakevault.betting.stats.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TenantSchemaNameTest {

	@Test
	void shouldBuildSchemaNameFromValidSlug() {
		TenantSchemaName schema = TenantSchemaName.fromSlug("acme");

		assertThat(schema.value()).isEqualTo("tenant_acme");
		assertThat(schema.slug()).isEqualTo("acme");
	}

	@Test
	void shouldRejectSlugWithUppercase() {
		assertThatThrownBy(() -> TenantSchemaName.fromSlug("ACME"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectSlugStartingWithDigit() {
		assertThatThrownBy(() -> TenantSchemaName.fromSlug("1acme"))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectNullSlug() {
		assertThatThrownBy(() -> TenantSchemaName.fromSlug(null))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void shouldRejectValueWithoutTenantPrefix() {
		assertThatThrownBy(() -> new TenantSchemaName("acme"))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
