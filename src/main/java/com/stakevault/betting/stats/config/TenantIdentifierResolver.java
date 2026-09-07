package com.stakevault.betting.stats.config;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {

	private static final String DEFAULT_SCHEMA = "public";

	@Override
	public String resolveCurrentTenantIdentifier() {
		var schema = TenantContextHolder.current();
		return schema == null ? DEFAULT_SCHEMA : schema.value();
	}

	@Override
	public boolean validateExistingCurrentSessions() {
		return true;
	}
}
