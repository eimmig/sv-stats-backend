package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
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

	private DimensionResolver resolver;

	@BeforeEach
	void setUp() {
		resolver = new DimensionResolver(bettingHouseRepository, sportRepository, leagueRepository, marketRepository,
				tipsterRepository, dateRepository);
	}

	@Test
	void shouldCreateBettingHouseWhenMissing() {
		UUID id = UUID.randomUUID();
		when(bettingHouseRepository.existsById(id)).thenReturn(false);

		UUID resolved = resolver.resolveBettingHouse(id, "House");

		assertThat(resolved).isEqualTo(id);
		verify(bettingHouseRepository).save(new DimBettingHouse(id, "House"));
	}

	@Test
	void shouldNotRecreateBettingHouseWhenAlreadyExists() {
		UUID id = UUID.randomUUID();
		when(bettingHouseRepository.existsById(id)).thenReturn(true);

		resolver.resolveBettingHouse(id, "House");

		verify(bettingHouseRepository, never()).save(any());
	}

	@Test
	void shouldReturnNullForTipsterWhenIdIsNull() {
		UUID resolved = resolver.resolveTipster(null, "Tipster");

		assertThat(resolved).isNull();
		verify(tipsterRepository, never()).existsById(any());
	}

	@Test
	void shouldReuseExistingDateRow() {
		UUID existingId = UUID.randomUUID();
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		when(dateRepository.findByDayAndMonthAndYear(6, 9, 2026))
				.thenReturn(Optional.of(new DimDate(existingId, 6, 9, 2026, 3, "SUNDAY")));

		UUID resolved = resolver.resolveDate(instant);

		assertThat(resolved).isEqualTo(existingId);
		verify(dateRepository, never()).save(any());
	}

	@Test
	void shouldCreateDateRowWithCorrectFieldsWhenMissing() {
		Instant instant = Instant.parse("2026-09-06T12:00:00Z");
		when(dateRepository.findByDayAndMonthAndYear(6, 9, 2026)).thenReturn(Optional.empty());
		when(dateRepository.save(any()))
				.thenAnswer(invocation -> invocation.getArgument(0));

		UUID resolved = resolver.resolveDate(instant);

		ArgumentCaptor<DimDate> captor = ArgumentCaptor.forClass(DimDate.class);
		verify(dateRepository).save(captor.capture());
		DimDate saved = captor.getValue();
		assertThat(resolved).isEqualTo(saved.id());
		assertThat(saved.day()).isEqualTo(6);
		assertThat(saved.month()).isEqualTo(9);
		assertThat(saved.year()).isEqualTo(2026);
		assertThat(saved.quarter()).isEqualTo(3);
		assertThat(saved.dayOfWeek()).isEqualTo("SUNDAY");
	}
}
