package com.stakevault.betting.stats.domain.port.out;

import java.util.List;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

public interface TenantSchemaGateway {

	boolean exists(TenantSchemaName schema);

	void createAndMigrate(TenantSchemaName schema);

	void migrateExistingOnly(TenantSchemaName schema);

	/** Schemas de organizacao que ja tem as somas diarias (migration da feat-036 aplicada). */
	List<TenantSchemaName> listWithDailyAggregates();

	/** Apaga o schema da organizacao e o que esta instancia guarda dele em memoria (epic-065). Nao falha sem schema. */
	void drop(TenantSchemaName schema);
}
