package com.stakevault.betting.stats.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TeamsControllerIntegrationTest extends TenantSchemaIntegrationSupport {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();
	private final DimSportRepository dimSportRepository;
	private final DimTeamRepository dimTeamRepository;
	private final ObjectMapper objectMapper;

	TeamsControllerIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			DimSportRepository dimSportRepository, DimTeamRepository dimTeamRepository, ObjectMapper objectMapper) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimSportRepository = dimSportRepository;
		this.dimTeamRepository = dimTeamRepository;
		this.objectMapper = objectMapper;
	}

	private HttpResponse<String> get(String query, String... headers) throws Exception {
		String uri = "http://localhost:" + port + "/api/v1/statistics/teams" + (query == null ? "" : "?" + query);
		HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(uri)).GET();
		for (int i = 0; i < headers.length; i += 2) {
			builder.header(headers[i], headers[i + 1]);
		}
		return httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
	}

	@Test
	void shouldReturn400WhenSportIdIsMissing() throws Exception {
		HttpResponse<String> response = get(null, "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(400);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.path("type").asString()).isEqualTo("https://docs/errors/statistics-search-missing-filter");
	}

	@Test
	void shouldReturnEmptyListWhenSportHasNoTeams() throws Exception {
		UUID sportId;
		try (var _ = TenantContextScope.open(schema)) {
			sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
		}

		HttpResponse<String> response = get("sportId=" + sportId, "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.isArray()).isTrue();
		assertThat(body).isEmpty();
	}

	@Test
	void shouldReturnTeamsScopedBySportOnly() throws Exception {
		UUID soccerId;
		UUID basketballId;
		try (var _ = TenantContextScope.open(schema)) {
			soccerId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			basketballId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Basketball")).id();
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Flamengo", soccerId));
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Vasco", soccerId));
			// mesmo nome, esporte diferente - nao deve aparecer na listagem de soccer.
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Flamengo", basketballId));
		}

		HttpResponse<String> response = get("sportId=" + soccerId, "X-Tenant-Id", tenantSlug);

		assertThat(response.statusCode()).isEqualTo(200);
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body).hasSize(2);
		assertThat(body.get(0).path("name").asString()).isEqualTo("Flamengo");
		assertThat(body.get(1).path("name").asString()).isEqualTo("Vasco");
	}
}
