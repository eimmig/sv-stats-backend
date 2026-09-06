package com.stakevault.betting.stats.adapter.in.web;

import java.net.URI;
import java.util.Locale;

import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.stakevault.betting.stats.domain.model.InvalidTenantSlugException;
import com.stakevault.betting.stats.domain.model.LocalizedDomainException;
import com.stakevault.betting.stats.domain.model.TenantAlreadyProvisionedException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class DomainExceptionHandler {

	private final MessageSource messageSource;

	public DomainExceptionHandler(MessageSource messageSource) {
		this.messageSource = messageSource;
	}

	@ExceptionHandler({ TenantAlreadyProvisionedException.class, InvalidTenantSlugException.class })
	public ProblemDetail handle(LocalizedDomainException exception, Locale locale, HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(
				HttpStatus.valueOf(exception.httpStatusCode()),
				ProblemDetailMessages.detail(exception, locale, messageSource));
		problem.setTitle(ProblemDetailMessages.title(exception, locale, messageSource));
		problem.setType(URI.create("https://docs/errors/" + ProblemDetailMessages.typeSlug(exception)));
		problem.setInstance(URI.create(request.getRequestURI()));
		return problem;
	}
}
