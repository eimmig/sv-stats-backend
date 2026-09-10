package com.stakevault.betting.stats.domain.port.in;

public interface ProvisionTenantSchemaUseCase {

	boolean exists(String tenantSlug);

	void ensureSchemaExists(String tenantSlug);

	void migrateIfPending(String tenantSlug);
}
