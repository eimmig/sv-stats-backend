package com.stakevault.betting.stats.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

@Configuration
public class LocaleConfig {

	private static final List<Locale> SUPPORTED_LOCALES = List.of(
			Locale.forLanguageTag("pt-BR"), Locale.forLanguageTag("en-US"), Locale.forLanguageTag("es"));
	private static final Locale DEFAULT_LOCALE = Locale.forLanguageTag("pt-BR");

	@Bean
	LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setSupportedLocales(SUPPORTED_LOCALES);
		resolver.setDefaultLocale(DEFAULT_LOCALE);
		return resolver;
	}
}
