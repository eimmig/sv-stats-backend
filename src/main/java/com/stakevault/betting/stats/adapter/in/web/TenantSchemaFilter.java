package com.stakevault.betting.stats.adapter.in.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.UriUtils;

import tools.jackson.databind.ObjectMapper;
import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.LocalizedDomainException;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.model.TenantSchemaNotFoundException;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantSchemaFilter extends OncePerRequestFilter {

	public static final String TENANT_HEADER = "X-Tenant-Id";
	private static final String TENANT_MDC_KEY = "tenantId";
	private static final LocalizedDomainException INVALID_TENANT_ID = new InvalidTenantIdHeader();
	private static final LocalizedDomainException MISSING_TENANT_ID = new MissingTenantIdHeader();

	private final ProvisionTenantSchemaUseCase provisionTenantSchema;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final ObjectMapper objectMapper;
	private final String adminPathPrefix;
	private final String actuatorPathPrefix;

	public TenantSchemaFilter(ProvisionTenantSchemaUseCase provisionTenantSchema, MessageSource messageSource,
			LocaleResolver localeResolver, ObjectMapper objectMapper,
			@Value("${admin.path-prefix:/api/v1/admin/}") String adminPathPrefix,
			@Value("${management.endpoints.web.base-path:/actuator}") String actuatorPathPrefix) {
		this.provisionTenantSchema = provisionTenantSchema;
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
		this.objectMapper = objectMapper;
		this.adminPathPrefix = adminPathPrefix;
		this.actuatorPathPrefix = actuatorPathPrefix;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String decodedPath = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
		return decodedPath.startsWith(adminPathPrefix) || decodedPath.startsWith(actuatorPathPrefix);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String tenantSlug = request.getHeader(TENANT_HEADER);
		if (tenantSlug == null || tenantSlug.isBlank()) {
			Locale locale = localeResolver.resolveLocale(request);
			FilterProblemWriter.write(response, request, objectMapper, MISSING_TENANT_ID.httpStatusCode(),
					ProblemDetailMessages.typeSlug(MISSING_TENANT_ID),
					ProblemDetailMessages.title(MISSING_TENANT_ID, locale, messageSource),
					ProblemDetailMessages.detail(MISSING_TENANT_ID, locale, messageSource));
			return;
		}

		TenantSchemaName schema;
		try {
			schema = TenantSchemaName.fromSlug(tenantSlug);
		} catch (IllegalArgumentException _) {
			Locale locale = localeResolver.resolveLocale(request);
			FilterProblemWriter.write(response, request, objectMapper, INVALID_TENANT_ID.httpStatusCode(),
					ProblemDetailMessages.typeSlug(INVALID_TENANT_ID),
					ProblemDetailMessages.title(INVALID_TENANT_ID, locale, messageSource),
					ProblemDetailMessages.detail(INVALID_TENANT_ID, locale, messageSource));
			return;
		}

		MDC.put(TENANT_MDC_KEY, schema.value());
		try {
			provisionTenantSchema.migrateIfPending(tenantSlug);
			try (var _ = TenantContextScope.open(schema)) {
				chain.doFilter(request, response);
			}
		} catch (TenantSchemaNotFoundException exception) {
			Locale locale = localeResolver.resolveLocale(request);
			FilterProblemWriter.write(response, request, objectMapper, exception.httpStatusCode(),
					ProblemDetailMessages.typeSlug(exception),
					ProblemDetailMessages.title(exception, locale, messageSource),
					ProblemDetailMessages.detail(exception, locale, messageSource));
		} finally {
			MDC.remove(TENANT_MDC_KEY);
		}
	}

	private static final class InvalidTenantIdHeader implements LocalizedDomainException {

		@Override
		public String messageKey() {
			return "error.invalid-tenant-id";
		}

		@Override
		public int httpStatusCode() {
			return HttpServletResponse.SC_BAD_REQUEST;
		}
	}

	private static final class MissingTenantIdHeader implements LocalizedDomainException {

		@Override
		public String messageKey() {
			return "error.missing-tenant-id";
		}

		@Override
		public int httpStatusCode() {
			return HttpServletResponse.SC_BAD_REQUEST;
		}
	}
}
