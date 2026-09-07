package com.stakevault.betting.stats.adapter.out.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.config.TenantContextHolder;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

import tools.jackson.databind.ObjectMapper;

@Repository
public class RedisMetricsCacheRepository implements MetricsCacheRepository {

	// Sem regra de negocio/RNF definindo TTL - rede de seguranca contra chave nunca evitada por
	// algum caminho esquecido; a invalidacao explicita no consumo do evento e o mecanismo
	// primario de frescor (ver plan_review de feat-005).
	private static final Duration SAFETY_NET_TTL = Duration.ofHours(1);

	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	public RedisMetricsCacheRepository(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<BetMetrics> findOverall() {
		return find(overallKey(), BetMetrics.class);
	}

	@Override
	public void saveOverall(BetMetrics metrics) {
		save(overallKey(), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findBySport() {
		return findSegmentList(segmentKey("sport"));
	}

	@Override
	public void saveBySport(List<SegmentedBetMetrics> metrics) {
		save(segmentKey("sport"), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByMarket() {
		return findSegmentList(segmentKey("market"));
	}

	@Override
	public void saveByMarket(List<SegmentedBetMetrics> metrics) {
		save(segmentKey("market"), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByBettingHouse() {
		return findSegmentList(segmentKey("house"));
	}

	@Override
	public void saveByBettingHouse(List<SegmentedBetMetrics> metrics) {
		save(segmentKey("house"), metrics);
	}

	@Override
	public Optional<BetMetrics> findMonthly(int year, int month) {
		return find(monthlyKey(year, month), BetMetrics.class);
	}

	@Override
	public void saveMonthly(int year, int month, BetMetrics metrics) {
		save(monthlyKey(year, month), metrics);
	}

	@Override
	public void evict(int year, int month) {
		redisTemplate.delete(
				List.of(overallKey(), segmentKey("sport"), segmentKey("market"), segmentKey("house"),
						monthlyKey(year, month)));
	}

	private Optional<List<SegmentedBetMetrics>> findSegmentList(String key) {
		String json = redisTemplate.opsForValue().get(key);
		if (json == null) {
			return Optional.empty();
		}
		return Optional.of(objectMapper.readValue(json,
				objectMapper.getTypeFactory().constructCollectionType(List.class, SegmentedBetMetrics.class)));
	}

	private <T> Optional<T> find(String key, Class<T> type) {
		String json = redisTemplate.opsForValue().get(key);
		return json == null ? Optional.empty() : Optional.of(objectMapper.readValue(json, type));
	}

	private void save(String key, Object value) {
		redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), SAFETY_NET_TTL);
	}

	private static String tenantSlug() {
		return TenantContextHolder.current().slug();
	}

	private static String overallKey() {
		return "tenant:" + tenantSlug() + ":dashboard:consolidated";
	}

	private static String segmentKey(String segment) {
		return "tenant:" + tenantSlug() + ":segment:" + segment;
	}

	private static String monthlyKey(int year, int month) {
		return "tenant:" + tenantSlug() + ":stats:monthly:" + year + "_" + month;
	}
}
