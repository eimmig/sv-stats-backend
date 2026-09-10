package com.stakevault.betting.stats.domain.model;

import java.util.regex.Pattern;

public record TenantSchemaName(String value) {

	private static final Pattern SLUG = Pattern.compile("^[a-z][a-z0-9-]{1,55}$");
	private static final String TENANT_PREFIX = "tenant_";

	public TenantSchemaName {
		if (value == null || !value.startsWith(TENANT_PREFIX)) {
			throw new IllegalArgumentException("invalid tenant schema name: " + value);
		}
	}

	public static TenantSchemaName fromSlug(String slug) {
		if (slug == null || !SLUG.matcher(slug).matches()) {
			throw new IllegalArgumentException("invalid tenant slug: " + slug);
		}
		return new TenantSchemaName(TENANT_PREFIX + slug);
	}

	public String slug() {
		return value.substring(TENANT_PREFIX.length());
	}
}
