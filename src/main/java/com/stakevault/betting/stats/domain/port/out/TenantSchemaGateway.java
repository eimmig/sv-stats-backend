package com.stakevault.betting.stats.domain.port.out;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

public interface TenantSchemaGateway {

	boolean exists(TenantSchemaName schema);

	void createAndMigrate(TenantSchemaName schema);

	void migrateExistingOnly(TenantSchemaName schema);
}
