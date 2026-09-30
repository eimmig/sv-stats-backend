package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.port.in.BetCreatedEvent;
import com.stakevault.betting.stats.domain.port.in.BetSettledEvent;
import com.stakevault.betting.stats.domain.port.in.ProcessBetEventUseCase;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class ProcessBetEventOrderIndependenceIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final Instant BET_DATE = Instant.parse("2026-09-06T15:00:00Z");
	private static final Instant SETTLED_AT = Instant.parse("2026-10-15T12:00:00Z");

	private final ProcessBetEventUseCase processBetEvent;
	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;

	ProcessBetEventOrderIndependenceIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, ProcessBetEventUseCase processBetEvent, FactBetRepository factBetRepository,
			DimDateRepository dimDateRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.processBetEvent = processBetEvent;
		this.factBetRepository = factBetRepository;
		this.dimDateRepository = dimDateRepository;
	}

	@Test
	void shouldReachTheSameFinalRowWhenCreatedIsProcessedFirst() {
		UUID betId = UUID.randomUUID();
		Dims dims = new Dims();

		try (var _ = TenantContextScope.open(schema)) {
			processBetEvent.processCreated(UUID.randomUUID(), created(betId, dims));
			processBetEvent.processSettled(UUID.randomUUID(), settled(betId, dims));

			assertFinalRow(betId);
		}
	}

	@Test
	void shouldReachTheSameFinalRowWhenSettledIsProcessedFirst() {
		UUID betId = UUID.randomUUID();
		Dims dims = new Dims();

		try (var _ = TenantContextScope.open(schema)) {
			processBetEvent.processSettled(UUID.randomUUID(), settled(betId, dims));
			processBetEvent.processCreated(UUID.randomUUID(), created(betId, dims));

			assertFinalRow(betId);
		}
	}

	private void assertFinalRow(UUID betId) {
		FactBet row = factBetRepository.findById(betId).orElseThrow();
		UUID betDateId = dimDateRepository.findByDayAndMonthAndYear(6, 9, 2026).orElseThrow().id();

		assertThat(row.dateId()).isEqualTo(betDateId);
		assertThat(row.betType()).isEqualTo(BetType.LIVE);
		assertThat(row.status()).isEqualTo(BetStatus.WON);
		assertThat(row.profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(row.isWin()).isTrue();
		assertThat(row.team1Id()).isNotNull();
	}

	private BetCreatedEvent created(UUID betId, Dims dims) {
		return new BetCreatedEvent(betId, dims.house, "House", dims.sport, "Sport", dims.league, "League", dims.market,
				"Market", null, null, null, "Flamengo", null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5),
				BetType.LIVE, BET_DATE);
	}

	private BetSettledEvent settled(UUID betId, Dims dims) {
		return new BetSettledEvent(betId, dims.house, "House", dims.sport, "Sport", dims.league, "League", dims.market,
				"Market", null, null, null, "Flamengo", null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5),
				BetStatus.WON, BigDecimal.valueOf(50), SETTLED_AT);
	}

	private static final class Dims {
		final UUID house = UUID.randomUUID();
		final UUID sport = UUID.randomUUID();
		final UUID league = UUID.randomUUID();
		final UUID market = UUID.randomUUID();
	}
}
