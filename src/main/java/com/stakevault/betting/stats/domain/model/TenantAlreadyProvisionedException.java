package com.stakevault.betting.stats.domain.model;

public class TenantAlreadyProvisionedException extends RuntimeException {

	private final String slug;

	public TenantAlreadyProvisionedException(String slug) {
		super("tenant already provisioned: " + slug);
		this.slug = slug;
	}

	public String slug() {
		return slug;
	}
}
