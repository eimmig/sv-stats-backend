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
class StatisticsSearchControllerIntegrationTest extends TenantSchemaIntegrationSupport {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository dimBettingHouseRepository;
	private final DimSportRepository dimSportRepository;
	private final DimLeagueRepository dimLeagueRepository;
	private final DimMarketRepository dimMarketRepository;
	private final ObjectMapper objectMapper;

	StatisticsSearchControllerIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, FactBetRepository factBetRepository, DimDateRepository dimDateRepository,
			DimBettingHouseRepository dimBettingHouseRepository, DimSportRepository dimSportRepository,
			DimLeagueRepository dimLeagueRepository, DimMarketRepository dimMarketRepository,
			ObjectMapper objectMapper) {
		super(provisionTenantSchema, jdbcTemplate);
		this.factBetRepository = factBetRepository;
		this.dimDateRepository = dimDateRepository;
		this.dimBettingHouseRepository = dimBettingHouseRepository;
		this.dimSportRepository = dimSportRepository;
		this.dimLeagueRepository = dimLeagueRepository;
		this.dimMarketRepository = dimMarketRepository;
		this.objectMapper = objectMapper;
	}

	private HttpResponse<String> get(String query, String... headers) throws Exception {
		String uri = "http://localhost:" + port + "/api/v1/statistics/search" + (query == null ? "" : "?" + query);
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri)).GET();
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void shouldReturn400LocalizedWhenSportIdIsMissing() throws Exception {
		HttpResponse<String> response = get("leagueId=" + UUID.randomUUID(), "X-Tenant-Id", tenantSlug,
				"Accept-Language", "pt-BR");

		assertThat(response.statusCode()).isEqualTo(400);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("type").asString()).isEqualTo("https://docs/errors/statistics-search-missing-filter");
		assertThat(body.path("detail").asString()).contains("sportId");
	}

	@Test
	void shouldReturn400WhenLeagueIdIsMissing() throws Exception {
		HttpResponse<String> response = get("sportId=" + UUID.randomUUID(), "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(400);
	}

	@Test
	void shouldReturnZeroedSummaryWhenCombinationHasNoSettledBets() throws Exception {
		try (var _ = TenantContextScope.open(schema)) {
			dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer"));
		}
		UUID sportId = UUID.randomUUID();
		UUID leagueId = UUID.randomUUID();

		HttpResponse<String> response = get("sportId=" + sportId + "&leagueId=" + leagueId, "X-Tenant-Id",
				tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("summary").path("betCount").asInt()).isZero();
		assertThat(body.path("summary").path("totalStaked").asDouble()).isEqualTo(0.0);
		assertThat(body.path("summary").path("roi").asDouble()).isEqualTo(0.0);
		assertThat(body.path("summary").path("avgOdd").isNull()).isTrue();
		assertThat(body.path("summary").path("sharpeRatio").isNull()).isTrue();
		assertThat(body.path("timeline").isArray()).isTrue();
		assertThat(body.path("timeline")).isEmpty();
	}

	@Test
	void shouldReturnFullSummaryAndTimelineForMatchingCombination() throws Exception {
		UUID sportId;
		UUID leagueId;
		try (var _ = TenantContextScope.open(schema)) {
			sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			leagueId = dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID date1 = dimDateRepository.save(new DimDate(UUID.randomUUID(), 1, 9, 2026, 3, "TUESDAY")).id();
			UUID date2 = dimDateRepository.save(new DimDate(UUID.randomUUID(), 5, 9, 2026, 3, "SATURDAY")).id();

			factBetRepository.save(new FactBet(UUID.randomUUID(), date1, houseId, sportId, leagueId, marketId, null,
					null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5), BigDecimal.valueOf(50), true,
					BetStatus.WON, null, 1));
			factBetRepository.save(new FactBet(UUID.randomUUID(), date2, houseId, sportId, leagueId, marketId, null,
					null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2.0), BigDecimal.valueOf(-100), false,
					BetStatus.LOST, null, 1));
		}

		HttpResponse<String> response = get("sportId=" + sportId + "&leagueId=" + leagueId, "X-Tenant-Id",
				tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("filters").path("sportId").asString()).isEqualTo(sportId.toString());
		assertThat(body.path("summary").path("betCount").asInt()).isEqualTo(2);
		assertThat(body.path("summary").path("totalStaked").asDouble()).isEqualTo(200.0);
		assertThat(body.path("summary").path("netProfit").asDouble()).isEqualTo(-50.0);
		assertThat(body.path("summary").path("avgOdd").asDouble()).isEqualTo(1.75);
		assertThat(body.path("summary").path("maxDrawdown").asDouble()).isEqualTo(100.0);
		assertThat(body.path("timeline")).hasSize(2);
		assertThat(body.path("timeline").get(0).path("cumulativeProfit").asDouble()).isEqualTo(50.0);
		assertThat(body.path("timeline").get(1).path("cumulativeProfit").asDouble()).isEqualTo(-50.0);
	}
}
