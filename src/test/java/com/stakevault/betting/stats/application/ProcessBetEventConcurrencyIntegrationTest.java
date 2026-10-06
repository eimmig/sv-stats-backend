package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

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

class ProcessBetEventConcurrencyIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final Instant BET_DATE = Instant.parse("2026-09-06T15:00:00Z");
	private static final Instant SETTLED_AT = Instant.parse("2026-10-15T12:00:00Z");
	private static final long TIMEOUT_SECONDS = 30;

	private final ProcessBetEventUseCase processBetEvent;
	private final DimDateRepository dimDateRepository;

	@MockitoSpyBean
	private FactBetRepository factBetRepository;

	ProcessBetEventConcurrencyIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, ProcessBetEventUseCase processBetEvent, DimDateRepository dimDateRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.processBetEvent = processBetEvent;
		this.dimDateRepository = dimDateRepository;
	}

	@Test
	void shouldKeepTheSettlementWhenCreatedReadsAnEmptyRowBeforeSettledCommits() throws Exception {
		UUID betId = UUID.randomUUID();
		Dims dims = new Dims();
		CountDownLatch createdHasRead = new CountDownLatch(1);
		CountDownLatch settledHasCommitted = new CountDownLatch(1);
		pauseFirstReadOf("created-consumer", createdHasRead, settledHasCommitted);

		ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "created-consumer"));
		try {
			Future<?> created = executor.submit(() -> {
				try (var _ = TenantContextScope.open(schema)) {
					processBetEvent.processCreated(UUID.randomUUID(), created(betId, dims));
				}
			});
			assertThat(createdHasRead.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

			try (var _ = TenantContextScope.open(schema)) {
				processBetEvent.processSettled(UUID.randomUUID(), settled(betId, dims));
			}
			settledHasCommitted.countDown();
			created.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		assertFinalRow(betId);
	}

	@Test
	void shouldKeepTheBetTypeWhenSettledReadsAnEmptyRowBeforeCreatedCommits() throws Exception {
		UUID betId = UUID.randomUUID();
		Dims dims = new Dims();
		CountDownLatch settledHasRead = new CountDownLatch(1);
		CountDownLatch createdHasCommitted = new CountDownLatch(1);
		pauseFirstReadOf("settled-consumer", settledHasRead, createdHasCommitted);

		ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> new Thread(runnable, "settled-consumer"));
		try {
			Future<?> settled = executor.submit(() -> {
				try (var _ = TenantContextScope.open(schema)) {
					processBetEvent.processSettled(UUID.randomUUID(), settled(betId, dims));
				}
			});
			assertThat(settledHasRead.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();

			try (var _ = TenantContextScope.open(schema)) {
				processBetEvent.processCreated(UUID.randomUUID(), created(betId, dims));
			}
			createdHasCommitted.countDown();
			settled.get(TIMEOUT_SECONDS, TimeUnit.SECONDS);
		} finally {
			executor.shutdownNow();
		}

		assertFinalRow(betId);
	}

	private void pauseFirstReadOf(String threadName, CountDownLatch reached, CountDownLatch resume) {
		doAnswer(invocation -> {
			Object result = invocation.callRealMethod();
			if (Thread.currentThread().getName().equals(threadName) && reached.getCount() > 0) {
				reached.countDown();
				resume.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
			}
			return result;
		}).when(factBetRepository).findById(any());
	}

	private void assertFinalRow(UUID betId) {
		try (var _ = TenantContextScope.open(schema)) {
			FactBet row = factBetRepository.findById(betId).orElseThrow();
			UUID betDateId = dimDateRepository.findByDayAndMonthAndYear(6, 9, 2026).orElseThrow().id();

			assertThat(row.status()).isEqualTo(BetStatus.WON);
			assertThat(row.profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
			assertThat(row.isWin()).isTrue();
			assertThat(row.betType()).isEqualTo(BetType.LIVE);
			assertThat(row.dateId()).isEqualTo(betDateId);
			assertThat(row.team1Id()).isNotNull();
		}
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
