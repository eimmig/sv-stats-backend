package com.stakevault.betting.stats.adapter.in.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UriUtils;

import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class AdminApiKeyFilter extends OncePerRequestFilter {

	public static final String ADMIN_API_KEY_HEADER = "X-Admin-Api-Key";

	private final String configuredApiKey;
	private final String adminPathPrefix;
	private final ObjectMapper objectMapper;

	public AdminApiKeyFilter(@Value("${admin.api-key}") String configuredApiKey,
			@Value("${admin.path-prefix:/api/v1/admin/}") String adminPathPrefix, ObjectMapper objectMapper) {
		this.configuredApiKey = configuredApiKey;
		this.adminPathPrefix = adminPathPrefix;
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
			FilterProblemWriter.write(response, request, objectMapper, HttpServletResponse.SC_UNAUTHORIZED,
					"invalid-admin-api-key", "Chave de administrador invalida",
					"O header X-Admin-Api-Key esta ausente ou nao confere.");
			return;
		}
		chain.doFilter(request, response);
	}

	private boolean constantTimeEquals(String provided, String configured) {
		return MessageDigest.isEqual(
				provided.getBytes(StandardCharsets.UTF_8), configured.getBytes(StandardCharsets.UTF_8));
	}
}
