package com.stakevault.betting.stats.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

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
import com.stakevault.betting.stats.support.DimDateFixtures;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

/**
 * Exemplo numerico da Secao 2.5 do TCC 2 (Tabelas "Apostas do exemplo numerico" e "Indicadores
 * calculados para o exemplo numerico"): as seis apostas do texto, gravadas na tabela fato, devem
 * produzir pela API os mesmos valores calculados a mao no texto.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkedExampleIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final String[] WEEKDAYS = { "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY" };

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

	WorkedExampleIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			FactBetRepository factBetRepository, DimDateRepository dimDateRepository,
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

	private JsonNode get(String pathAndQuery) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + pathAndQuery))
				.header("X-Tenant-Id", tenantSlug).GET().build();
		HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
		assertThat(response.statusCode()).isEqualTo(200);
		return objectMapper.readTree(response.body());
	}

	@Test
	void shouldReproduceTheWorkedExampleOfTheThesis() throws Exception {
		// aposta i: stake, odd, lucro, situacao (uma por dia, na ordem do texto)
		Object[][] bets = {
				{ "100", "2.00", "100", BetStatus.WON },
				{ "100", "1.80", "-100", BetStatus.LOST },
				{ "50", "2.50", "75", BetStatus.WON },
				{ "100", "1.50", "-100", BetStatus.LOST },
				{ "50", "3.00", "0", BetStatus.VOID },
				{ "100", "1.90", "90", BetStatus.WON } };
		UUID sportId;
		UUID leagueId;
		try (var _ = TenantContextScope.open(schema)) {
			sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			leagueId = dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			for (int i = 0; i < bets.length; i++) {
				UUID dateId = DimDateFixtures.ensure(dimDateRepository, i + 1, 9, 2026, 3, WEEKDAYS[i]);
				BetStatus status = (BetStatus) bets[i][3];
				factBetRepository.save(new FactBet(UUID.randomUUID(), dateId, houseId, sportId, leagueId, marketId,
						null, null, null, new BigDecimal((String) bets[i][0]), new BigDecimal((String) bets[i][1]),
						new BigDecimal((String) bets[i][2]), status == BetStatus.WON, status, null, 1));
			}
		}

		JsonNode overall = get("/api/v1/statistics").path("overall");
		assertThat(overall.path("netProfit").decimalValue()).isEqualByComparingTo("65");
		assertThat(overall.path("totalStaked").decimalValue()).isEqualByComparingTo("500");
		assertThat(overall.path("roi").decimalValue()).isEqualByComparingTo("0.1300");
		assertThat(overall.path("winRate").decimalValue()).isEqualByComparingTo("0.6000");
		assertThat(overall.path("avgOdd").asDouble()).isCloseTo(2.1167, within(0.0001));
		assertThat(overall.path("impliedProbability").decimalValue()).isEqualByComparingTo("0.5297");
		assertThat(overall.path("edge").decimalValue()).isEqualByComparingTo("0.0703");
		assertThat(overall.path("winRateLower").decimalValue()).isEqualByComparingTo("0.2307");
		assertThat(overall.path("winRateUpper").decimalValue()).isEqualByComparingTo("0.8824");
		assertThat(overall.path("roiLower").decimalValue()).isEqualByComparingTo("-0.7684");
		assertThat(overall.path("roiUpper").decimalValue()).isEqualByComparingTo("1.0284");

		JsonNode summary = get("/api/v1/statistics/search?sportId=" + sportId + "&leagueId=" + leagueId)
				.path("summary");
		assertThat(summary.path("maxDrawdown").decimalValue()).isEqualByComparingTo("125");
		assertThat(summary.path("sharpeRatio").decimalValue()).isEqualByComparingTo("0.1168");
	}
}
