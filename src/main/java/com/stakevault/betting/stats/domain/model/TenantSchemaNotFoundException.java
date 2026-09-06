package com.stakevault.betting.stats.domain.model;

public class TenantSchemaNotFoundException extends RuntimeException implements LocalizedDomainException {

	public TenantSchemaNotFoundException(TenantSchemaName schema) {
		super("tenant schema not provisioned: " + schema.value());
	}

	@Override
	public String messageKey() {
		return "error.tenant-not-found";
	}

	@Override
	public int httpStatusCode() {
		return 404;
	}
}
