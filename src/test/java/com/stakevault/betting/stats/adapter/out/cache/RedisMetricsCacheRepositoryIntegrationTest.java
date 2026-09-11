package com.stakevault.betting.stats.adapter.out.cache;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import com.stakevault.betting.stats.TestcontainersConfiguration;
import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class RedisMetricsCacheRepositoryIntegrationTest {

	private final MetricsCacheRepository cacheRepository;
	private final StringRedisTemplate redisTemplate;

	RedisMetricsCacheRepositoryIntegrationTest(MetricsCacheRepository cacheRepository,
			StringRedisTemplate redisTemplate) {
		this.cacheRepository = cacheRepository;
		this.redisTemplate = redisTemplate;
	}

	private TenantSchemaName newTenant() {
		return TenantSchemaName.fromSlug("test-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12));
	}

	private BetMetrics sampleMetrics() {
		return new BetMetrics(BigDecimal.valueOf(100), BigDecimal.valueOf(50), BigDecimal.valueOf(0.5),
				BigDecimal.valueOf(0.5), 1, 1, 0, 0, 0, 0, BigDecimal.valueOf(1.92));
	}

	@Test
	void shouldReturnEmptyOnCacheMiss() {
		try (var _ = TenantContextScope.open(newTenant())) {
			assertThat(cacheRepository.findOverall()).isEmpty();
			assertThat(cacheRepository.findBySport()).isEmpty();
			assertThat(cacheRepository.findMonthly(2026, 9)).isEmpty();
		}
	}

	@Test
	void shouldReturnSavedValueOnCacheHit() {
		try (var _ = TenantContextScope.open(newTenant())) {
			BetMetrics metrics = sampleMetrics();
			cacheRepository.saveOverall(metrics);

			BetMetrics found = cacheRepository.findOverall().orElseThrow();

			assertThat(found.totalStaked()).isEqualByComparingTo(metrics.totalStaked());
			assertThat(found.roi()).isEqualByComparingTo(metrics.roi());
			assertThat(found.settledCount()).isEqualTo(metrics.settledCount());
		}
	}

	@Test
	void shouldRoundTripSegmentedList() {
		try (var _ = TenantContextScope.open(newTenant())) {
			String sportId = UUID.randomUUID().toString();
			List<SegmentedBetMetrics> segments = List.of(new SegmentedBetMetrics(sportId, "Soccer", sampleMetrics()));

			cacheRepository.saveBySport(segments);
			cacheRepository.saveByMarket(segments);
			cacheRepository.saveByBettingHouse(segments);
			cacheRepository.saveByBetType(segments);

			assertThat(cacheRepository.findBySport().orElseThrow()).hasSize(1);
			assertThat(cacheRepository.findBySport().orElseThrow().get(0).dimensionId()).isEqualTo(sportId);
			assertThat(cacheRepository.findByMarket().orElseThrow()).hasSize(1);
			assertThat(cacheRepository.findByBettingHouse().orElseThrow()).hasSize(1);
			assertThat(cacheRepository.findByBetType().orElseThrow()).hasSize(1);
		}
	}

	@Test
	void shouldEvictOverallSegmentsAndTheGivenMonth() {
		try (var _ = TenantContextScope.open(newTenant())) {
			List<SegmentedBetMetrics> segments = List.of();
			cacheRepository.saveOverall(sampleMetrics());
			cacheRepository.saveBySport(segments);
			cacheRepository.saveByMarket(segments);
			cacheRepository.saveByBettingHouse(segments);
			cacheRepository.saveByBetType(segments);
			cacheRepository.saveMonthly(2026, 9, sampleMetrics());

			cacheRepository.evict(2026, 9);

			assertThat(cacheRepository.findOverall()).isEmpty();
			assertThat(cacheRepository.findBySport()).isEmpty();
			assertThat(cacheRepository.findByMarket()).isEmpty();
			assertThat(cacheRepository.findByBettingHouse()).isEmpty();
			assertThat(cacheRepository.findByBetType()).isEmpty();
			assertThat(cacheRepository.findMonthly(2026, 9)).isEmpty();
		}
	}

	@Test
	void shouldEvictOnlyTheGivenMonthNotOtherMonths() {
		try (var _ = TenantContextScope.open(newTenant())) {
			cacheRepository.saveMonthly(2026, 8, sampleMetrics());
			cacheRepository.saveMonthly(2026, 9, sampleMetrics());

			cacheRepository.evict(2026, 9);

			assertThat(cacheRepository.findMonthly(2026, 8)).isPresent();
			assertThat(cacheRepository.findMonthly(2026, 9)).isEmpty();
		}
	}

	@Test
	void shouldIsolateCacheKeysBetweenTenants() {
		TenantSchemaName tenantA = newTenant();
		TenantSchemaName tenantB = newTenant();

		try (var _ = TenantContextScope.open(tenantA)) {
			cacheRepository.saveOverall(sampleMetrics());
		}

		try (var _ = TenantContextScope.open(tenantB)) {
			assertThat(cacheRepository.findOverall()).isEmpty();
		}
		try (var _ = TenantContextScope.open(tenantA)) {
			assertThat(cacheRepository.findOverall()).isPresent();
		}
	}

	@Test
	void shouldStoreValueUnderTheDocumentedKeyPattern() {
		TenantSchemaName tenant = newTenant();
		try (var _ = TenantContextScope.open(tenant)) {
			cacheRepository.saveOverall(sampleMetrics());
			cacheRepository.saveMonthly(2026, 9, sampleMetrics());
		}

		assertThat(redisTemplate.hasKey("tenant:" + tenant.slug() + ":dashboard:consolidated")).isTrue();
		assertThat(redisTemplate.hasKey("tenant:" + tenant.slug() + ":stats:monthly:2026_9")).isTrue();
	}
}
