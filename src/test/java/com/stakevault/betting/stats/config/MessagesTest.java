package com.stakevault.betting.stats.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;

class MessagesTest {

	private final ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();

	MessagesTest() {
		messageSource.setBasename("messages");
		messageSource.setFallbackToSystemLocale(false);
		messageSource.setDefaultEncoding("UTF-8");
	}

	@Test
	void shouldResolveTenantNotFoundInAllThreeLocales() {
		assertThat(messageSource.getMessage("error.tenant-not-found.title", null, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("Tenant não encontrado");
		assertThat(messageSource.getMessage("error.tenant-not-found.title", null, Locale.forLanguageTag("en-US")))
				.isEqualTo("Tenant not found");
		assertThat(messageSource.getMessage("error.tenant-not-found.title", null, Locale.forLanguageTag("es")))
				.isEqualTo("Tenant no encontrado");
	}

	@Test
	void shouldResolveInvalidTenantIdInAllThreeLocales() {
		assertThat(messageSource.getMessage("error.invalid-tenant-id.title", null, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("Tenant inválido");
		assertThat(messageSource.getMessage("error.invalid-tenant-id.title", null, Locale.forLanguageTag("en-US")))
				.isEqualTo("Invalid tenant");
		assertThat(messageSource.getMessage("error.invalid-tenant-id.title", null, Locale.forLanguageTag("es")))
				.isEqualTo("Tenant inválido");
	}

	@Test
	void shouldResolveInvalidAdminApiKeyInAllThreeLocales() {
		assertThat(messageSource.getMessage("error.invalid-admin-api-key.title", null, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("Chave de administrador inválida");
		assertThat(messageSource.getMessage("error.invalid-admin-api-key.title", null, Locale.forLanguageTag("en-US")))
				.isEqualTo("Invalid admin key");
		assertThat(messageSource.getMessage("error.invalid-admin-api-key.title", null, Locale.forLanguageTag("es")))
				.isEqualTo("Clave de administrador inválida");
	}

	@Test
	void shouldResolveTenantAlreadyProvisionedWithSlugArgumentInAllThreeLocales() {
		Object[] args = { "acme" };
		assertThat(messageSource.getMessage("error.tenant-already-provisioned.detail", args, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("Já existe um tenant provisionado para o slug \"acme\".");
		assertThat(messageSource.getMessage("error.tenant-already-provisioned.detail", args, Locale.forLanguageTag("en-US")))
				.isEqualTo("A tenant is already provisioned for slug \"acme\".");
		assertThat(messageSource.getMessage("error.tenant-already-provisioned.detail", args, Locale.forLanguageTag("es")))
				.isEqualTo("Ya existe un tenant aprovisionado para el slug \"acme\".");
	}

	@Test
	void shouldResolveInvalidTenantSlugWithSlugArgumentInAllThreeLocales() {
		Object[] args = { "1invalid" };
		assertThat(messageSource.getMessage("error.invalid-tenant-slug.detail", args, Locale.forLanguageTag("pt-BR")))
				.isEqualTo("O slug \"1invalid\" não é um identificador de tenant válido.");
		assertThat(messageSource.getMessage("error.invalid-tenant-slug.detail", args, Locale.forLanguageTag("en-US")))
				.isEqualTo("The slug \"1invalid\" is not a valid tenant identifier.");
		assertThat(messageSource.getMessage("error.invalid-tenant-slug.detail", args, Locale.forLanguageTag("es")))
				.isEqualTo("El slug \"1invalid\" no es un identificador de tenant válido.");
	}
}
