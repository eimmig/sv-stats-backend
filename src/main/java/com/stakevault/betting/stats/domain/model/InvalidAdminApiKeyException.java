package com.stakevault.betting.stats.domain.model;

public class InvalidAdminApiKeyException extends RuntimeException implements LocalizedDomainException {

	public InvalidAdminApiKeyException() {
		super("missing or invalid X-Admin-Api-Key");
	}

	@Override
	public String messageKey() {
		return "error.invalid-admin-api-key";
	}

	@Override
	public int httpStatusCode() {
		return 401;
	}
}
