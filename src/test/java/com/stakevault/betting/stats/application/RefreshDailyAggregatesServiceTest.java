package com.stakevault.betting.stats.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.domain.port.out.DailyAggregateRepository;

@ExtendWith(MockitoExtension.class)
class RefreshDailyAggregatesServiceTest {

	@Mock
	private DailyAggregateRepository dailyAggregateRepository;

	private RefreshDailyAggregatesService service;

	@BeforeEach
	void setUp() {
		service = new RefreshDailyAggregatesService(dailyAggregateRepository);
	}

	@Test
	void shouldLeaveTheDirtyDaysAloneWhenAnotherReplicaHoldsTheLock() {
		when(dailyAggregateRepository.tryLock()).thenReturn(false);

		assertThat(service.refreshDirtyDays()).isZero();

		verify(dailyAggregateRepository, never()).takeDirtyDays();
		verify(dailyAggregateRepository, never()).rebuild(any());
	}

	@Test
	void shouldNotRebuildWhenNoDayIsDirty() {
		when(dailyAggregateRepository.tryLock()).thenReturn(true);
		when(dailyAggregateRepository.takeDirtyDays()).thenReturn(List.of());

		assertThat(service.refreshDirtyDays()).isZero();

		verify(dailyAggregateRepository, never()).rebuild(any());
	}

	@Test
	void shouldRebuildEveryDirtyDay() {
		List<LocalDate> days = List.of(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2));
		when(dailyAggregateRepository.tryLock()).thenReturn(true);
		when(dailyAggregateRepository.takeDirtyDays()).thenReturn(days);

		assertThat(service.refreshDirtyDays()).isEqualTo(2);

		verify(dailyAggregateRepository).rebuild(days);
	}
}
