package com.stakevault.betting.stats.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.stakevault.betting.stats.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class HealthChecksTest {

	@LocalServerPort
	private int port;

	private final HttpClient httpClient = HttpClient.newHttpClient();

	private HttpResponse<String> get(String path) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	@ParameterizedTest
	@ValueSource(strings = { "/actuator/health", "/actuator/health/liveness" })
	void shouldRespondUp(String path) throws Exception {
		HttpResponse<String> response = get(path);

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"");
	}

	@Test
	void shouldRespondUpAndIncludePostgresCheckForReadiness() throws Exception {
		HttpResponse<String> response = get("/actuator/health/readiness");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"status\":\"UP\"").contains("\"db\"");
	}

	@Test
	void shouldIncludeRabbitCheckForReadiness() throws Exception {
		HttpResponse<String> response = get("/actuator/health/readiness");

		assertThat(response.statusCode()).isEqualTo(200);
		assertThat(response.body()).contains("\"rabbit\"");
	}
}
