package com.stakevault.betting.stats.domain.model;

public class InvalidTenantSlugException extends RuntimeException implements LocalizedDomainException {

	private final String slug;

	public InvalidTenantSlugException(String slug, Throwable cause) {
		super("invalid tenant slug: " + slug, cause);
		this.slug = slug;
	}

	public String slug() {
		return slug;
	}

	@Override
	public String messageKey() {
		return "error.invalid-tenant-slug";
	}

	@Override
	public int httpStatusCode() {
		return 422;
	}

	@Override
	public Object[] messageArgs() {
		return new Object[] { slug == null ? "" : slug };
	}
}
