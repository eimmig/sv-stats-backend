package com.stakevault.betting.stats.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import tools.jackson.databind.ObjectMapper;
import com.stakevault.betting.stats.config.TenantContextHolder;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.model.TenantSchemaNotFoundException;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;

class TenantSchemaFilterTest {

	private final ProvisionTenantSchemaUseCase provisionTenantSchema = mock(ProvisionTenantSchemaUseCase.class);
	private final TenantSchemaFilter filter = new TenantSchemaFilter(provisionTenantSchema, new ObjectMapper());

	@Test
	void shouldPassThroughWithoutHeader() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		verifyNoInteractions(provisionTenantSchema);
		assertThat(TenantContextHolder.current()).isNull();
	}

	@Test
	void shouldReturn400ForInvalidTenantSlugWithoutCallingUseCase() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TenantSchemaFilter.TENANT_HEADER, "ACME!!!");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(400);
		assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(response.getContentAsString()).contains("\"type\":\"https://docs/errors/invalid-tenant-id\"");
		verifyNoInteractions(provisionTenantSchema);
	}

	@Test
	void shouldReturn404WhenTenantIsNotProvisioned() throws Exception {
		doThrow(new TenantSchemaNotFoundException(new TenantSchemaName("tenant_acme")))
				.when(provisionTenantSchema).migrateIfPending("acme");
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TenantSchemaFilter.TENANT_HEADER, "acme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		filter.doFilter(request, response, chain);

		assertThat(response.getStatus()).isEqualTo(404);
		assertThat(response.getContentType()).isEqualTo(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		assertThat(response.getContentAsString()).contains("\"type\":\"https://docs/errors/tenant-not-found\"");
	}

	@Test
	void shouldResolveContextDuringTheCallAndClearItAfter() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(TenantSchemaFilter.TENANT_HEADER, "acme");
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain() {
			@Override
			public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
				assertThat(TenantContextHolder.current()).isEqualTo(new TenantSchemaName("tenant_acme"));
			}
		};

		filter.doFilter(request, response, chain);

		verify(provisionTenantSchema).migrateIfPending("acme");
		assertThat(TenantContextHolder.current()).isNull();
	}
}
