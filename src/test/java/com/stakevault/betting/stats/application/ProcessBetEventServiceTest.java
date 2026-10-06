package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
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
		return createdEvent(betId, Instant.now());
	}

	private BetCreatedEvent createdEvent(UUID betId, Instant betDate) {
		return new BetCreatedEvent(betId, UUID.randomUUID(), "House", UUID.randomUUID(), "Sport", UUID.randomUUID(),
				"League", UUID.randomUUID(), "Market", null, null, null, "Team A", null, "Team B",
				BigDecimal.valueOf(100), BigDecimal.valueOf(2), BetType.PRE, betDate);
	}

	private BetSettledEvent settledEvent(UUID betId, Instant settledAt) {
		return settledEvent(betId, settledAt, null, "Team A", null, "Team B");
	}

	private BetSettledEvent settledEvent(UUID betId, Instant settledAt, UUID team1Id, String team1Name, UUID team2Id,
			String team2Name) {
		return new BetSettledEvent(betId, UUID.randomUUID(), "House", UUID.randomUUID(), "Sport", UUID.randomUUID(),
				"League", UUID.randomUUID(), "Market", null, null, team1Id, team1Name, team2Id, team2Name,
				BigDecimal.valueOf(100), BigDecimal.valueOf(2), BetStatus.WON, BigDecimal.valueOf(50), settledAt);
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
		when(factBetRepository.insertIfAbsent(any())).thenReturn(true);
		when(dimensionResolver.resolveDate(any())).thenReturn(dateId);
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processCreated(eventId, createdEvent(betId));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).insertIfAbsent(captor.capture());
		verify(factBetRepository, never()).save(any());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.PENDING);
		assertThat(captor.getValue().profit()).isNull();
		assertThat(captor.getValue().dateId()).isEqualTo(dateId);
		assertThat(captor.getValue().betType()).isEqualTo(BetType.PRE);
		verify(processedEventRepository).save(any());
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
	void shouldCompleteBetTypeAndDateWhenCreatedArrivesAfterAnEarlySettle() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID settledDateId = UUID.randomUUID();
		UUID createdDateId = UUID.randomUUID();
		UUID houseId = UUID.randomUUID();
		Instant createdAt = Instant.parse("2026-09-01T10:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, settledDateId, houseId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
						null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2), BigDecimal.valueOf(50), true,
						BetStatus.WON, null, 1)));
		when(dimensionResolver.resolveDate(any())).thenReturn(createdDateId);

		service.processCreated(eventId, createdEvent(betId, createdAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		FactBet saved = captor.getValue();
		assertThat(saved.betType()).isEqualTo(BetType.PRE);
		assertThat(saved.dateId()).isEqualTo(createdDateId);
		assertThat(saved.status()).isEqualTo(BetStatus.WON);
		assertThat(saved.profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(saved.isWin()).isTrue();
		assertThat(saved.bettingHouseId()).isEqualTo(houseId);
		verify(processedEventRepository).save(any());
		var betDate = createdAt.atZone(ZoneOffset.UTC).toLocalDate();
		verify(metricsCacheRepository).evict(betDate.getYear(), betDate.getMonthValue());
	}

	@Test
	void shouldUpdateAnAlreadyExistingPendingFactBetWhenCreatedArrivesAgain() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID dateId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, dateId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, null, null, BigDecimal.valueOf(50), BigDecimal.valueOf(1.5), null,
						null, BetStatus.PENDING, BetType.PRE, 1)));
		when(dimensionResolver.resolveDate(any())).thenReturn(dateId);
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processCreated(eventId, createdEvent(betId));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().stake()).isEqualByComparingTo(BigDecimal.valueOf(100));
		assertThat(captor.getValue().odd()).isEqualByComparingTo(BigDecimal.valueOf(2));
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.PENDING);
		verify(processedEventRepository).save(any());
	}

	@Test
	void shouldInsertFactBetAndMarkProcessedOnSettled() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-09-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.insertIfAbsent(any())).thenReturn(true);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).insertIfAbsent(captor.capture());
		verify(factBetRepository, never()).save(any());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.WON);
		assertThat(captor.getValue().profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(captor.getValue().isWin()).isTrue();
		verify(processedEventRepository).save(any());
		var settledDate = settledAt.atZone(ZoneOffset.UTC).toLocalDate();
		verify(metricsCacheRepository).evict(settledDate.getYear(), settledDate.getMonthValue());
	}

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

	@Test
	void shouldLeaveBetTypeNullWhenSettledArrivesBeforeCreated() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty());
		when(factBetRepository.insertIfAbsent(any())).thenReturn(true);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).insertIfAbsent(captor.capture());
		verify(factBetRepository, never()).save(any());
		assertThat(captor.getValue().betType()).isNull();
	}

	@Test
	void shouldPreserveExistingTeamIdsWhenSettledEventDoesNotCarryThem() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID existingTeam1Id = UUID.randomUUID();
		UUID existingTeam2Id = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.of(
				new FactBet(betId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, existingTeam1Id, existingTeam2Id, BigDecimal.valueOf(100),
						BigDecimal.valueOf(2), null, null, BetStatus.PENDING, BetType.PRE, 1)));
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, settledAt, null, null, null, null));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().team1Id()).isEqualTo(existingTeam1Id);
		assertThat(captor.getValue().team2Id()).isEqualTo(existingTeam2Id);
		verify(dimensionResolver, never()).resolveTeam(any(), any(), any());
	}

	@Test
	void shouldResolveTeamDimensionWhenSettledEventCarriesIt() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID catalogTeam1Id = UUID.randomUUID();
		Instant settledAt = Instant.parse("2026-10-15T12:00:00Z");
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty());
		when(factBetRepository.insertIfAbsent(any())).thenReturn(true);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveTeam(eq(catalogTeam1Id), eq("Flamengo"), any())).thenReturn(catalogTeam1Id);

		service.processSettled(eventId,
				settledEvent(betId, settledAt, catalogTeam1Id, "Flamengo", null, null));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).insertIfAbsent(captor.capture());
		verify(factBetRepository, never()).save(any());
		assertThat(captor.getValue().team1Id()).isEqualTo(catalogTeam1Id);
	}

	@Test
	void shouldCompleteTheCommittedSettledRowWhenCreatedLosesTheInsertRace() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID settledTeamId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty(), Optional.of(
				new FactBet(betId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, settledTeamId, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2),
						BigDecimal.valueOf(50), true, BetStatus.WON, null, 1)));
		when(factBetRepository.insertIfAbsent(any())).thenReturn(false);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processCreated(eventId, createdEvent(betId));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.WON);
		assertThat(captor.getValue().profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
		assertThat(captor.getValue().betType()).isEqualTo(BetType.PRE);
		assertThat(captor.getValue().team1Id()).isEqualTo(settledTeamId);
		verify(processedEventRepository).save(any());
	}

	@Test
	void shouldMergeTheCommittedCreatedRowWhenSettledLosesTheInsertRace() {
		UUID eventId = UUID.randomUUID();
		UUID betId = UUID.randomUUID();
		UUID createdDateId = UUID.randomUUID();
		when(processedEventRepository.existsByEventId(eventId)).thenReturn(false);
		when(factBetRepository.findById(betId)).thenReturn(Optional.empty(), Optional.of(
				new FactBet(betId, createdDateId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
						UUID.randomUUID(), null, null, null, BigDecimal.valueOf(100), BigDecimal.valueOf(2), null, null,
						BetStatus.PENDING, BetType.LIVE, 1)));
		when(factBetRepository.insertIfAbsent(any())).thenReturn(false);
		when(dimensionResolver.resolveDate(any())).thenReturn(UUID.randomUUID());
		when(dimensionResolver.resolveBettingHouse(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveSport(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveLeague(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));
		when(dimensionResolver.resolveMarket(any(), any())).thenAnswer(invocation -> invocation.getArgument(0));

		service.processSettled(eventId, settledEvent(betId, Instant.parse("2026-10-15T12:00:00Z")));

		ArgumentCaptor<FactBet> captor = ArgumentCaptor.forClass(FactBet.class);
		verify(factBetRepository).save(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(BetStatus.WON);
		assertThat(captor.getValue().betType()).isEqualTo(BetType.LIVE);
		assertThat(captor.getValue().dateId()).isEqualTo(createdDateId);
		verify(processedEventRepository).save(any());
	}
}
