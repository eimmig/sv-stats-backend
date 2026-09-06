package com.stakevault.betting.stats.domain.model;

public class InvalidTenantSlugException extends RuntimeException {

	private final String slug;

	public InvalidTenantSlugException(String slug, Throwable cause) {
		super("invalid tenant slug: " + slug, cause);
		this.slug = slug;
	}

	public String slug() {
		return slug;
	}
}
