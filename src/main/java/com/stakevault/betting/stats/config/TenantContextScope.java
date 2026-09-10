package com.stakevault.betting.stats.config;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

public final class TenantContextScope implements AutoCloseable {

	private TenantContextScope() {
	}

	public static TenantContextScope open(TenantSchemaName schema) {
		TenantContextHolder.set(schema);
		return new TenantContextScope();
	}

	@Override
	public void close() {
		TenantContextHolder.clear();
	}
}
