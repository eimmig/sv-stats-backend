package com.stakevault.betting.stats.domain.model;

public class TenantAlreadyProvisionedException extends RuntimeException implements LocalizedDomainException {

	private final String slug;

	public TenantAlreadyProvisionedException(String slug) {
		super("tenant already provisioned: " + slug);
		this.slug = slug;
	}

	public String slug() {
		return slug;
	}

	@Override
	public String messageKey() {
		return "error.tenant-already-provisioned";
	}

	@Override
	public int httpStatusCode() {
		return 409;
	}

	@Override
	public Object[] messageArgs() {
		return new Object[] { slug == null ? "" : slug };
	}
}
