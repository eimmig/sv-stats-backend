package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.ProcessedEvent;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaProcessedEventRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final ProcessedEventRepository processedEventRepository;

	JpaProcessedEventRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, ProcessedEventRepository processedEventRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.processedEventRepository = processedEventRepository;
	}

	@Test
	void shouldSaveAndReportExistingByEventId() {
		UUID eventId = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			assertThat(processedEventRepository.existsByEventId(eventId)).isFalse();

			processedEventRepository.save(new ProcessedEvent(UUID.randomUUID(), eventId, Instant.now()));

			assertThat(processedEventRepository.existsByEventId(eventId)).isTrue();
		}
	}

	@Test
	void shouldRejectDuplicateEventIdAtTheDatabaseLevel() {
		UUID eventId = UUID.randomUUID();

		try (var _ = TenantContextScope.open(schema)) {
			processedEventRepository.save(new ProcessedEvent(UUID.randomUUID(), eventId, Instant.now()));

			assertThatThrownBy(
					() -> processedEventRepository.save(new ProcessedEvent(UUID.randomUUID(), eventId, Instant.now())))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}
}
