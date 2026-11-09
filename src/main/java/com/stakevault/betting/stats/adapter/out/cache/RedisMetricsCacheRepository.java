package com.stakevault.betting.stats.adapter.out.cache;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.config.TenantContextHolder;
import com.stakevault.betting.stats.domain.model.BetMetrics;
import com.stakevault.betting.stats.domain.model.MonthlyBetMetrics;
import com.stakevault.betting.stats.domain.model.SegmentedBetMetrics;
import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;

import tools.jackson.databind.ObjectMapper;

@Repository
public class RedisMetricsCacheRepository implements MetricsCacheRepository {

	private static final Duration SAFETY_NET_TTL = Duration.ofHours(1);
	// a geracao muda a cada evento; entradas de geracoes antigas so esperam este prazo para sumir
	private static final Duration FILTERED_DASHBOARD_TTL = Duration.ofMinutes(10);

	private static final String KEY_PREFIX = "tenant:";
	private static final String SPORT_SEGMENT = "sport";
	private static final String MARKET_SEGMENT = "market";
	private static final String HOUSE_SEGMENT = "house";
	private static final String LEAGUE_SEGMENT = "league";
	private static final String TIPSTER_SEGMENT = "tipster";
	private static final String TEAM_SEGMENT = "team";
	private static final String BET_TYPE_SEGMENT = "byBetType";

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
	public Optional<List<SegmentedBetMetrics>> findByLeague() {
		return findSegmentList(segmentKey(LEAGUE_SEGMENT));
	}

	@Override
	public void saveByLeague(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(LEAGUE_SEGMENT), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByTipster() {
		return findSegmentList(segmentKey(TIPSTER_SEGMENT));
	}

	@Override
	public void saveByTipster(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(TIPSTER_SEGMENT), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByTeam() {
		return findSegmentList(segmentKey(TEAM_SEGMENT));
	}

	@Override
	public void saveByTeam(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(TEAM_SEGMENT), metrics);
	}

	@Override
	public Optional<List<SegmentedBetMetrics>> findByBetType() {
		return findSegmentList(segmentKey(BET_TYPE_SEGMENT));
	}

	@Override
	public void saveByBetType(List<SegmentedBetMetrics> metrics) {
		save(segmentKey(BET_TYPE_SEGMENT), metrics);
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
	public Optional<List<MonthlyBetMetrics>> findMonthlySeries() {
		String json = redisTemplate.opsForValue().get(monthlySeriesKey());
		if (json == null) {
			return Optional.empty();
		}
		return Optional.of(objectMapper.readValue(json,
				objectMapper.getTypeFactory().constructCollectionType(List.class, MonthlyBetMetrics.class)));
	}

	@Override
	public void saveMonthlySeries(List<MonthlyBetMetrics> series) {
		save(monthlySeriesKey(), series);
	}

	@Override
	public Optional<StatisticsDashboard> findDashboard(StatisticsFilter filter) {
		return find(filteredDashboardKey(filter), StatisticsDashboard.class);
	}

	@Override
	public void saveDashboard(StatisticsFilter filter, StatisticsDashboard dashboard) {
		redisTemplate.opsForValue().set(filteredDashboardKey(filter), objectMapper.writeValueAsString(dashboard),
				FILTERED_DASHBOARD_TTL);
	}

	@Override
	public void evict(int year, int month) {
		redisTemplate.opsForValue().increment(filteredGenerationKey());
		redisTemplate.delete(
				List.of(overallKey(), segmentKey(SPORT_SEGMENT), segmentKey(MARKET_SEGMENT), segmentKey(HOUSE_SEGMENT),
						segmentKey(LEAGUE_SEGMENT), segmentKey(TIPSTER_SEGMENT), segmentKey(TEAM_SEGMENT),
						segmentKey(BET_TYPE_SEGMENT),
						monthlySeriesKey(), monthlyKey(year, month)));
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

	private static String monthlySeriesKey() {
		return KEY_PREFIX + tenantSlug() + ":dashboard:monthly";
	}

	private static String filteredGenerationKey() {
		return KEY_PREFIX + tenantSlug() + ":dashboard:filtered:generation";
	}

	/**
	 * Uma chave por combinacao de filtros, prefixada pela geracao corrente: o evict so incrementa a
	 * geracao, e um calculo que comecou antes dele grava na geracao velha, que ninguem mais le.
	 */
	private String filteredDashboardKey(StatisticsFilter filter) {
		String generation = redisTemplate.opsForValue().get(filteredGenerationKey());
		return KEY_PREFIX + tenantSlug() + ":dashboard:filtered:" + (generation == null ? "0" : generation) + ":"
				+ filter.from() + ":" + filter.to() + ":" + filter.bettingHouseId() + ":" + filter.sportId() + ":"
				+ filter.leagueId() + ":" + filter.marketId() + ":" + filter.tipsterId();
	}

	private static String monthlyKey(int year, int month) {
		return KEY_PREFIX + tenantSlug() + ":stats:monthly:" + year + "_" + month;
	}
}
