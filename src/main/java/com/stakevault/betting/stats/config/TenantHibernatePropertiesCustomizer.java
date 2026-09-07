package com.stakevault.betting.stats.config;

import java.util.Map;

import org.hibernate.cfg.MultiTenancySettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

@Component
public class TenantHibernatePropertiesCustomizer implements HibernatePropertiesCustomizer {

	private final SchemaMultiTenantConnectionProvider connectionProvider;
	private final TenantIdentifierResolver identifierResolver;

	public TenantHibernatePropertiesCustomizer(SchemaMultiTenantConnectionProvider connectionProvider,
			TenantIdentifierResolver identifierResolver) {
		this.connectionProvider = connectionProvider;
		this.identifierResolver = identifierResolver;
	}

	@Override
	public void customize(Map<String, Object> hibernateProperties) {
		hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_CONNECTION_PROVIDER, connectionProvider);
		hibernateProperties.put(MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER, identifierResolver);
	}
}
