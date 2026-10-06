package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;

@ExtendWith(MockitoExtension.class)
class DimensionResolverTest {

	@Mock
	private DimBettingHouseRepository bettingHouseRepository;
	@Mock
	private DimSportRepository sportRepository;
	@Mock
	private DimLeagueRepository leagueRepository;
	@Mock
	private DimMarketRepository marketRepository;
	@Mock
	private DimTipsterRepository tipsterRepository;
	@Mock
	private DimDateRepository dateRepository;
	@Mock
	private DimTeamRepository teamRepository;

	private DimensionResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new DimensionResolver(bettingHouseRepository, sportRepository, leagueRepository, marketRepository,
				tipsterRepository, dateRepository, teamRepository);
	}

	@Test
	void shouldInsertEveryEventDimensionIgnoringConflictAndReturnTheEventId() {
		UUID houseId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		UUID leagueId = UUID.randomUUID();
		UUID marketId = UUID.randomUUID();
		UUID tipsterId = UUID.randomUUID();

		assertThat(resolver.resolveBettingHouse(houseId, "House")).isEqualTo(houseId);
		assertThat(resolver.resolveSport(sportId, "Sport")).isEqualTo(sportId);
		assertThat(resolver.resolveLeague(leagueId, "League")).isEqualTo(leagueId);
		assertThat(resolver.resolveMarket(marketId, "Market")).isEqualTo(marketId);
		assertThat(resolver.resolveTipster(tipsterId, "Tipster")).isEqualTo(tipsterId);

		verify(bettingHouseRepository).insertIfAbsent(new DimBettingHouse(houseId, "House"));
		verify(sportRepository).insertIfAbsent(new DimSport(sportId, "Sport"));
		verify(leagueRepository).insertIfAbsent(new DimLeague(leagueId, "League"));
		verify(marketRepository).insertIfAbsent(new DimMarket(marketId, "Market"));
		verify(tipsterRepository).insertIfAbsent(new DimTipster(tipsterId, "Tipster"));
		verify(bettingHouseRepository, never()).save(any());
	}

	@Test
	void shouldReturnNullForTipsterWhenIdIsNull() {
		UUID resolved = resolver.resolveTipster(null, "Tipster");

		assertThat(resolved).isNull();
		verify(tipsterRepository, never()).insertIfAbsent(any());
	}

	@Test
	void shouldReuseExistingDateRow() {
		UUID existingId = UUID.randomUUID();
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		when(dateRepository.findByDayAndMonthAndYear(6, 9, 2026))
				.thenReturn(Optional.of(new DimDate(existingId, 6, 9, 2026, 3, "SUNDAY")));

		UUID resolved = resolver.resolveDate(instant);

		assertThat(resolved).isEqualTo(existingId);
		verify(dateRepository, never()).insertIfAbsent(any());
	}

	@Test
	void shouldInsertDateRowWithCorrectFieldsWhenMissing() {
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		AtomicReference<DimDate> inserted = new AtomicReference<>();
		doAnswer(invocation -> {
			inserted.set(invocation.getArgument(0));
			return null;
		}).when(dateRepository).insertIfAbsent(any());
		when(dateRepository.findByDayAndMonthAndYear(6, 9, 2026)).thenReturn(Optional.empty())
				.thenAnswer(invocation -> Optional.of(inserted.get()));

		UUID resolved = resolver.resolveDate(instant);

		DimDate saved = inserted.get();
		assertThat(resolved).isEqualTo(saved.id());
		assertThat(saved.day()).isEqualTo(6);
		assertThat(saved.month()).isEqualTo(9);
		assertThat(saved.year()).isEqualTo(2026);
		assertThat(saved.quarter()).isEqualTo(3);
		assertThat(saved.dayOfWeek()).isEqualTo("SUNDAY");
	}

	@Test
	void shouldReturnTheRowThatWonTheRaceWhenAnotherConsumerInsertedTheSameDay() {
		UUID winnerId = UUID.randomUUID();
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		when(dateRepository.findByDayAndMonthAndYear(6, 9, 2026)).thenReturn(Optional.empty())
				.thenReturn(Optional.of(new DimDate(winnerId, 6, 9, 2026, 3, "SUNDAY")));

		UUID resolved = resolver.resolveDate(instant);

		assertThat(resolved).isEqualTo(winnerId);
		ArgumentCaptor<DimDate> captor = ArgumentCaptor.forClass(DimDate.class);
		verify(dateRepository).insertIfAbsent(captor.capture());
		assertThat(captor.getValue().id()).isNotEqualTo(winnerId);
	}

	@Test
	void shouldReturnNullForTeamWhenNameIsNull() {
		UUID sportId = UUID.randomUUID();

		UUID resolved = resolver.resolveTeam(UUID.randomUUID(), null, sportId);

		assertThat(resolved).isNull();
		verify(teamRepository, never()).findByNameAndSportId(any(), any());
	}

	@Test
	void shouldReuseExistingTeamByNameAndSportEvenWithoutIdInTheEvent() {
		UUID existingId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		when(teamRepository.findByNameAndSportId("Flamengo", sportId))
				.thenReturn(Optional.of(new DimTeam(existingId, "Flamengo", sportId)));

		UUID resolved = resolver.resolveTeam(null, "Flamengo", sportId);

		assertThat(resolved).isEqualTo(existingId);
		verify(teamRepository, never()).insertIfAbsent(any());
	}

	@Test
	void shouldPreferTheAlreadyPersistedIdOverTheEventIdOnNameAndSportCollision() {
		UUID localLegacyId = UUID.randomUUID();
		UUID catalogId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		when(teamRepository.findByNameAndSportId("Flamengo", sportId))
				.thenReturn(Optional.of(new DimTeam(localLegacyId, "Flamengo", sportId)));

		UUID resolved = resolver.resolveTeam(catalogId, "Flamengo", sportId);

		assertThat(resolved).isEqualTo(localLegacyId);
		verify(teamRepository, never()).insertIfAbsent(any());
	}

	@Test
	void shouldInsertTeamRowWithTheEventIdWhenSeenForTheFirstTime() {
		UUID catalogId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		when(teamRepository.findByNameAndSportId("Flamengo", sportId)).thenReturn(Optional.empty())
				.thenReturn(Optional.of(new DimTeam(catalogId, "Flamengo", sportId)));

		UUID resolved = resolver.resolveTeam(catalogId, "Flamengo", sportId);

		assertThat(resolved).isEqualTo(catalogId);
		verify(teamRepository).insertIfAbsent(new DimTeam(catalogId, "Flamengo", sportId));
	}

	@Test
	void shouldInsertTeamRowWithARandomIdWhenTheEventHasNoId() {
		UUID sportId = UUID.randomUUID();
		AtomicReference<DimTeam> inserted = new AtomicReference<>();
		doAnswer(invocation -> {
			inserted.set(invocation.getArgument(0));
			return null;
		}).when(teamRepository).insertIfAbsent(any());
		when(teamRepository.findByNameAndSportId("Flamengo", sportId)).thenReturn(Optional.empty())
				.thenAnswer(invocation -> Optional.of(inserted.get()));

		UUID resolved = resolver.resolveTeam(null, "Flamengo", sportId);

		DimTeam saved = inserted.get();
		assertThat(resolved).isEqualTo(saved.id());
		assertThat(saved.name()).isEqualTo("Flamengo");
		assertThat(saved.sportId()).isEqualTo(sportId);
	}

	@Test
	void shouldReturnTheTeamThatWonTheRaceWhenAnotherConsumerInsertedTheSameNameAndSport() {
		UUID winnerId = UUID.randomUUID();
		UUID sportId = UUID.randomUUID();
		when(teamRepository.findByNameAndSportId("Flamengo", sportId)).thenReturn(Optional.empty())
				.thenReturn(Optional.of(new DimTeam(winnerId, "Flamengo", sportId)));

		UUID resolved = resolver.resolveTeam(UUID.randomUUID(), "Flamengo", sportId);

		assertThat(resolved).isEqualTo(winnerId);
	}

	@Test
	void shouldNotReuseTeamFromADifferentSport() {
		UUID soccerSportId = UUID.randomUUID();
		UUID basketballSportId = UUID.randomUUID();
		AtomicReference<DimTeam> inserted = new AtomicReference<>();
		doAnswer(invocation -> {
			inserted.set(invocation.getArgument(0));
			return null;
		}).when(teamRepository).insertIfAbsent(any());
		when(teamRepository.findByNameAndSportId("Flamengo", basketballSportId)).thenReturn(Optional.empty())
				.thenAnswer(invocation -> Optional.of(inserted.get()));

		resolver.resolveTeam(null, "Flamengo", basketballSportId);

		verify(teamRepository, never()).findByNameAndSportId("Flamengo", soccerSportId);
		assertThat(inserted.get().sportId()).isEqualTo(basketballSportId);
	}
}
