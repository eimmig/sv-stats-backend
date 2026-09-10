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

	private static final String KEY_PREFIX = "tenant:";
	private static final String SPORT_SEGMENT = "sport";
	private static final String MARKET_SEGMENT = "market";
	private static final String HOUSE_SEGMENT = "house";

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
		return findSegmentList(segmentKey(SPORT_SEGMENT));
	}

	@Override
	public void saveBySport(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(SPORT_SEGMENT), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByMarket() {
		return findSegmentList(segmentKey(MARKET_SEGMENT));
	}

	@Override
	public void saveByMarket(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(MARKET_SEGMENT), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByBettingHouse() {
		return findSegmentList(segmentKey(HOUSE_SEGMENT));
	}

	@Override
	public void saveByBettingHouse(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(HOUSE_SEGMENT), metrics);
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
				List.of(overallKey(), segmentKey(SPORT_SEGMENT), segmentKey(MARKET_SEGMENT), segmentKey(HOUSE_SEGMENT),
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
		return KEY_PREFIX + tenantSlug() + ":dashboard:consolidated";
	}

	private static String segmentKey(String segment) {
		return KEY_PREFIX + tenantSlug() + ":segment:" + segment;
	}

	private static String monthlyKey(int year, int month) {
		return KEY_PREFIX + tenantSlug() + ":stats:monthly:" + year + "_" + month;
	}
}
