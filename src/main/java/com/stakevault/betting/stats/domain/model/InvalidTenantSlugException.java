package com.stakevault.betting.stats.domain.model;

public class InvalidTenantSlugException extends LocalizedRuntimeException {

	private final String slug;

	public InvalidTenantSlugException(String slug, Throwable cause) {
		super("invalid tenant slug: " + slug, cause, slug == null ? "" : slug);
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
}
