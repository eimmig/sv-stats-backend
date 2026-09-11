package com.stakevault.betting.stats.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StatisticsControllerIntegrationTest extends TenantSchemaIntegrationSupport {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository dimBettingHouseRepository;
	private final DimSportRepository dimSportRepository;
	private final DimLeagueRepository dimLeagueRepository;
	private final DimMarketRepository dimMarketRepository;
	private final StringRedisTemplate redisTemplate;
	private final ObjectMapper objectMapper;

	StatisticsControllerIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			FactBetRepository factBetRepository, DimDateRepository dimDateRepository,
			DimBettingHouseRepository dimBettingHouseRepository, DimSportRepository dimSportRepository,
			DimLeagueRepository dimLeagueRepository, DimMarketRepository dimMarketRepository,
			StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
		super(provisionTenantSchema, jdbcTemplate);
		this.factBetRepository = factBetRepository;
		this.dimDateRepository = dimDateRepository;
		this.dimBettingHouseRepository = dimBettingHouseRepository;
		this.dimSportRepository = dimSportRepository;
		this.dimLeagueRepository = dimLeagueRepository;
		this.dimMarketRepository = dimMarketRepository;
		this.redisTemplate = redisTemplate;
		this.objectMapper = objectMapper;
	}

	private HttpResponse<String> get(String query, String... headers) throws Exception {
		String uri = "http://localhost:" + port + "/api/v1/statistics" + (query == null ? "" : "?" + query);
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri)).GET();
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	private UUID seedSettledBet(String sportName) {
		try (var _ = TenantContextScope.open(schema)) {
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), sportName)).id();
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID leagueId = dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
			UUID dateId = dimDateRepository.save(new DimDate(UUID.randomUUID(), 6, 9, 2026, 3, "SUNDAY")).id();
			factBetRepository.save(new FactBet(UUID.randomUUID(), dateId, houseId, sportId, leagueId, marketId, null,
					null, null, BigDecimal.valueOf(100), null, BigDecimal.valueOf(50), true, BetStatus.WON, null, 1));
			return sportId;
		}
	}

	@Test
	void shouldReturnBundleWithoutFilterAndPopulateTheCache() throws Exception {
		seedSettledBet("Soccer");

		HttpResponse<String> response = get(null, "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("overall").path("totalStaked").asDouble()).isEqualTo(100.0);
		assertThat(body.path("overall").path("settledCount").asInt()).isEqualTo(1);
		// Cada item de segmento/mes aninha as metricas sob "metrics" (mesmos records de dominio
		// ja usados desde feat-004/005), nao achatado - ver docs/API-CONTRACTS.md.
		JsonNode sportSegment = body.path("bySport").get(0);
		assertThat(sportSegment.path("dimensionName").asString()).isEqualTo("Soccer");
		assertThat(sportSegment.path("metrics").path("totalStaked").asDouble()).isEqualTo(100.0);
		assertThat(body.path("byMarket").get(0).path("metrics").path("settledCount").asInt()).isEqualTo(1);
		assertThat(body.path("byBettingHouse").get(0).path("metrics").path("settledCount").asInt()).isEqualTo(1);
		JsonNode monthEntry = body.path("monthly").get(0);
		assertThat(monthEntry.path("year").asInt()).isEqualTo(2026);
		assertThat(monthEntry.path("month").asInt()).isEqualTo(9);
		assertThat(monthEntry.path("metrics").path("settledCount").asInt()).isEqualTo(1);
		try (var _ = TenantContextScope.open(schema)) {
			assertThat(redisTemplate.hasKey("tenant:" + tenantSlug + ":dashboard:consolidated")).isTrue();
		}
	}

	@Test
	void shouldRestrictResultsWhenFilteredBySportId() throws Exception {
		UUID soccerId = seedSettledBet("Soccer");
		seedSettledBet("Tennis");

		HttpResponse<String> response = get("sportId=" + soccerId, "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("overall").path("settledCount").asInt()).isEqualTo(1);
		assertThat(body.path("bySport")).hasSize(1);
		assertThat(body.path("bySport").get(0).path("dimensionId").asString()).isEqualTo(soccerId.toString());
	}

	@Test
	void shouldReturn400WhenTenantIdHeaderIsMissing() throws Exception {
		HttpResponse<String> response = get(null);

		assertThat(response.statusCode()).isEqualTo(400);
	}
}
