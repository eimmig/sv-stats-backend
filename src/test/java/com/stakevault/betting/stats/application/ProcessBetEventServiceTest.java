package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.port.in.BetCreatedEvent;
import com.stakevault.betting.stats.domain.port.in.BetSettledEvent;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;

@ExtendWith(MockitoExtension.class)
class ProcessBetEventServiceTest {

	@Mock
	private FactBetRepository factBetRepository;
	@Mock
	private ProcessedEventRepository processedEventRepository;
	@Mock
	private DimensionResolver dimensionResolver;
	@Mock
	private MetricsCacheRepository metricsCacheRepository;

	private ProcessBetEventService service;

	@BeforeEach
	void setUp() {
		service = new ProcessBetEventService(factBetRepository, processedEventRepository, dimensionResolver,
				metricsCacheRepository);
	}

	private BetCreatedEvent createdEvent(UUID betId) {
		return new BetCreatedEvent(betId, UUID.randomUUID(), "House", UUID.randomUUID(), "Sport", UUID.randomUUID(),
				"League", UUID.randomUUID(), "Market", null, null, "Team A", "Team B", BigDecimal.valueOf(100),
				BigDecimal.valueOf(2), BetType.PRE, Instant.now());
	}

	private BetSettledEvent settledEvent(UUID betId, Instant settledAt) {
		return new BetSettledEvent(betId, UUID.randomUUID(), "House", UUID.randomUUID(), "Sport", UUID.randomUUID(),
				"League", UUID.randomUUID(), "Market", null, null, "Team A", "Team B", BigDecimal.valueOf(100),
				BigDecimal.valueOf(2), BetStatus.WON, BigDecimal.valueOf(50), settledAt);
	}

	@Test
	void shouldSkipEntirelyWhenEventIdAlreadyProcessed() {
		UUID eventId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(true);

		service.processCreated(eventId, createdEvent(UUID.randomUUID()));

		verify(factBetRepository, never()).save(any());
		verify(dimensionResolver, never()).resolveBettingHouse(any(), any());
		verify(processedEventRepository, never()).save(any());
		verify(metricsCacheRepository, never()).evict(anyInt(), anyInt());
	}

	@Test
	void shouldInsertPendingFactBetAndMarkProcessedOnCreated() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty());
		UUID dateId = UUID.randomUUID();
		when(dimensionResolver.resolveDate(any())).thenReturn(dateId);
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processCreated(eventId, createdEvent(betId));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.PENDING);
		assertThat(captor.getValue().profit()).isNull();
		assertThat(captor.getValue().dateId()).isEqualTo(dateId);
		assertThat(captor.getValue().betType()).isEqualTo(BetType.PRE);
		verify(processedEventRepository).save(any());
		// RN06 exclui pending de toda agregacao - inserir uma linha pending e invisivel pras
		// metricas cacheadas, evitar aqui seria desperdicio.
		verify(metricsCacheRepository, never()).evict(anyInt(), anyInt());
	}

	@Test
	void shouldNotOverwriteAnAlreadySettledFactBetWhenCreatedArrivesLate() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, null, null, BigDecimal.valueOf(100), null, BigDecimal.valueOf(50),
						true, BetStatus.WON, BetType.PRE, 1)));

		service.processCreated(eventId, createdEvent(betId));

		verify(factBetRepository, never()).save(any());
		verify(processedEventRepository).save(any());
		verify(metricsCacheRepository, never()).evict(anyInt(), anyInt());
	}

	@Test
	void shouldUpsertFactBetAndMarkProcessedOnSettled() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-09-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.WON);
		assertThat(captor.getValue().profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(captor.getValue().isWin()).isTrue();
		verify(processedEventRepository).save(any());
		// evict usa o mes de settledAt (RN06 - so a liquidacao muda o que as queries enxergam).
		var settledDate = settledAt.atZone(ZoneOffset.UTC).toLocalDate();
		verify(metricsCacheRepository).evict(settledDate.getYear(), settledDate.getMonthValue());
	}

	// Achado real do plan review de epic-011: dateId reflete a data do JOGO (betDate), resolvida
	// por processCreated - processSettled nao pode recalcula-lo a partir de settledAt (mes de
	// liquidacao pode divergir do mes do jogo), sob risco de corromper o agregado mensal do
	// dashboard (feat-006) silenciosamente.
	@Test
	void shouldPreserveExistingDateIdWhenSettlingAnAlreadyCreatedBet() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID gameDateId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, gameDateId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2), null,
						null, BetStatus.PENDING, BetType.LIVE, 1)));
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().dateId()).isEqualTo(gameDateId);
		verify(dimensionResolver, never()).resolveDate(any());
	}

	// epic-014: BetSettledEvent nao carrega betType (so BetCreated tem esse campo) - processSettled
	// precisa preservar o valor ja gravado no insert, nao perde-lo/anula-lo a cada liquidacao.
	@Test
	void shouldPreserveExistingBetTypeWhenSettlingAnAlreadyCreatedBet() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2), null,
						null, BetStatus.PENDING, BetType.PRE, 1)));
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().betType()).isEqualTo(BetType.PRE);
	}

	// BetSettled chegando antes do BetCreated correspondente (mensagens fora de ordem) - sem linha
	// existente, betType nasce null (so BetCreated grava esse campo).
	@Test
	void shouldLeaveBetTypeNullWhenSettledArrivesBeforeCreated() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty());
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().betType()).isNull();
	}
}
