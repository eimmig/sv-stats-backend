package com.stakevault.betting.stats.config;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

public final class TenantContextHolder {

	private static final ThreadLocal<TenantSchemaName> CURRENT = new ThreadLocal<>();

	private TenantContextHolder() {
	}

	static void set(TenantSchemaName schema) {
		CURRENT.set(schema);
	}

	static void clear() {
		CURRENT.remove();
	}

	public static TenantSchemaName current() {
		return CURRENT.get();
	}
}
