package com.stakevault.betting.stats.adapter.in.web;

import java.io.IOException;
import java.util.Locale;

import org.slf4j.MDC;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;

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

	private final ProvisionTenantSchemaUseCase provisionTenantSchema;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final ObjectMapper objectMapper;

	public TenantSchemaFilter(ProvisionTenantSchemaUseCase provisionTenantSchema, MessageSource messageSource,
			LocaleResolver localeResolver, ObjectMapper objectMapper) {
		this.provisionTenantSchema = provisionTenantSchema;
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
		this.objectMapper = objectMapper;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String tenantSlug = request.getHeader(TENANT_HEADER);
		if (tenantSlug == null || tenantSlug.isBlank()) {
			chain.doFilter(request, response);
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
}
