package com.stakevault.betting.stats.domain.port.out;

import java.util.List;
import java.util.Optional;

import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;

// Cache-aside (chaves tenant:{slug}:... - ver docs/services/stats-service.md "Cache Redis").
// Nenhum metodo recebe o tenant explicitamente: o adapter resolve via TenantContextHolder,
// mesma simetria que TenantIdentifierResolver ja usa pra rotear o schema do Hibernate.
public interface MetricsCacheRepository {

	Optional<BetMetrics> findOverall();

	void saveOverall(BetMetrics metrics);

	Optional<List<SegmentedBetMetrics>> findBySport();

	void saveBySport(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByMarket();

	void saveByMarket(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByBettingHouse();

	void saveByBettingHouse(List<SegmentedBetMetrics> metrics);

	Optional<List<SegmentedBetMetrics>> findByBetType();

	void saveByBetType(List<SegmentedBetMetrics> metrics);

	Optional<BetMetrics> findMonthly(int year, int month);

	void saveMonthly(int year, int month, BetMetrics metrics);

	// Invalidacao no consumo do evento (BetCreated/BetSettled) - evita as 5 chaves do tenant,
	// incluindo o mes especifico do evento, em vez de confiar so no TTL de seguranca.
	void evict(int year, int month);
}
