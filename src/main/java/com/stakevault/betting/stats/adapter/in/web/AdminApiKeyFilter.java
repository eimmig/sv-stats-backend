package com.stakevault.betting.stats.adapter.in.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.UriUtils;

import tools.jackson.databind.ObjectMapper;
import com.stakevault.betting.stats.domain.model.InvalidAdminApiKeyException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

	public static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

	private final String configuredApiKey;
	private final String adminPathPrefix;
	private final MessageSource messageSource;
	private final LocaleResolver localeResolver;
	private final ObjectMapper objectMapper;

	public AdminApiKeyFilter(@Value("${admin.api-key}") String configuredApiKey,
			@Value("${admin.path-prefix:/api/v1/admin/}") String adminPathPrefix, MessageSource messageSource,
			LocaleResolver localeResolver, ObjectMapper objectMapper) {
		this.configuredApiKey = configuredApiKey;
		this.adminPathPrefix = adminPathPrefix;
		this.messageSource = messageSource;
		this.localeResolver = localeResolver;
		this.objectMapper = objectMapper;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String decodedPath = UriUtils.decode(request.getRequestURI(), StandardCharsets.UTF_8);
		return !decodedPath.startsWith(adminPathPrefix);
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String providedKey = request.getHeader(ADMIN_API_KEY_HEADER);
		if (providedKey == null || !constantTimeEquals(providedKey, configuredApiKey)) {
			writeUnauthorized(request, response);
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean constantTimeEquals(String provided, String configured) {
		return MessageDigest.isEqual(
				provided.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8));
	}

	private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response) throws IOException {
		InvalidAdminApiKeyException exception = new InvalidAdminApiKeyException();
		Locale locale = localeResolver.resolveLocale(request);
		String title = ProblemDetailMessages.title(exception, locale, messageSource);
		String detail = ProblemDetailMessages.detail(exception, locale, messageSource);
		FilterProblemWriter.write(response, request, objectMapper, exception.httpStatusCode(),
				ProblemDetailMessages.typeSlug(exception), title, detail);
	}
}
