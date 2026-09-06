package com.stakevault.betting.stats.adapter.in.web;

import java.util.Locale;

import org.springframework.context.MessageSource;

import com.stakevault.betting.stats.domain.model.LocalizedDomainException;

final class ProblemDetailMessages {

	private ProblemDetailMessages() {
	}

	static String title(LocalizedDomainException exception, Locale locale, MessageSource messageSource) {
		return messageSource.getMessage(exception.messageKey() + ".title", exception.messageArgs(), locale);
	}

	static String detail(LocalizedDomainException exception, Locale locale, MessageSource messageSource) {
		return messageSource.getMessage(exception.messageKey() + ".detail", exception.messageArgs(), locale);
	}

	static String typeSlug(LocalizedDomainException exception) {
		return exception.messageKey().replaceFirst("^error\\.", "");
	}
}
