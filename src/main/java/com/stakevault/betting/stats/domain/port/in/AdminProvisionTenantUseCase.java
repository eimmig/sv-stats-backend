package com.stakevault.betting.stats.domain.port.in;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

public interface AdminProvisionTenantUseCase {

	TenantSchemaName provisionTenant(String slug);
}
