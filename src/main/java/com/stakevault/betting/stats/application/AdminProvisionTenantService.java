package com.stakevault.betting.stats.application;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.InvalidTenantSlugException;
import com.stakevault.betting.stats.domain.model.TenantAlreadyProvisionedException;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.in.AdminProvisionTenantUseCase;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;

@Service
public class AdminProvisionTenantService implements AdminProvisionTenantUseCase {

	private final ProvisionTenantSchemaUseCase provisionTenantSchema;
	private final TenantSchemaGateway gateway;
	private final MetricsCacheRepository cache;

	public AdminProvisionTenantService(ProvisionTenantSchemaUseCase provisionTenantSchema, TenantSchemaGateway gateway,
			MetricsCacheRepository cache) {
		this.provisionTenantSchema = provisionTenantSchema;
		this.gateway = gateway;
		this.cache = cache;
	}

	@Override
	public TenantSchemaName provisionTenant(String slug) {
		TenantSchemaName schema;
		try {
			schema = TenantSchemaName.fromSlug(slug);
		} catch (IllegalArgumentException cause) {
			throw new InvalidTenantSlugException(slug, cause);
		}

		if (provisionTenantSchema.exists(slug)) {
			throw new TenantAlreadyProvisionedException(slug);
		}
		provisionTenantSchema.ensureSchemaExists(slug);
		return schema;
	}

	@Override
	public void deleteTenant(String slug) {
		TenantSchemaName schema;
		try {
			schema = TenantSchemaName.fromSlug(slug);
		} catch (IllegalArgumentException cause) {
			throw new InvalidTenantSlugException(slug, cause);
		}
		gateway.drop(schema);
		cache.deleteAllFor(slug);
	}
}
