package com.stakevault.betting.stats.adapter.in.web;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stakevault.betting.stats.domain.model.InvalidTenantSlugException;
import com.stakevault.betting.stats.domain.model.TenantAlreadyProvisionedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class DomainExceptionHandler {

	@ExceptionHandler(TenantAlreadyProvisionedException.class)
	public ProblemDetail handleAlreadyProvisioned(TenantAlreadyProvisionedException exception,
			HttpServletRequest request) {
		return problem(HttpStatus.CONFLICT, "tenant-already-provisioned", "Tenant ja provisionado",
				"Tenant ja provisionado: " + exception.slug(), request);
	}

	@ExceptionHandler(InvalidTenantSlugException.class)
	public ProblemDetail handleInvalidSlug(InvalidTenantSlugException exception, HttpServletRequest request) {
		return problem(HttpStatus.UNPROCESSABLE_ENTITY, "invalid-tenant-slug", "Slug de tenant invalido",
				"Slug de tenant invalido: " + (exception.slug() == null ? "" : exception.slug()), request);
	}

	private ProblemDetail problem(HttpStatus status, String typeSlug, String title, String detail,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		problem.setType(URI.create("https://docs/errors/" + typeSlug));
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}
}
