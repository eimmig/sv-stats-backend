package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.stakevault.betting.stats.domain.model.StatisticsFilter;

class JpaFactBetRepositoryMonthOrderTest {

	private static MonthlyAggregateProjection month(int year, int month) {
		MonthlyAggregateProjection projection = mock(MonthlyAggregateProjection.class);
		when(projection.getYear()).thenReturn(year);
		when(projection.getMonth()).thenReturn(month);
		return projection;
	}

	@Test
	void shouldReturnTheMonthlySeriesInChronologicalOrderWhateverOrderTheDatabaseUses() {
		FactBetSpringDataRepository facts = mock(FactBetSpringDataRepository.class);
		DailySegmentAggregateSpringDataRepository dailySums = mock(DailySegmentAggregateSpringDataRepository.class);
		doReturn(List.of(month(2026, 9), month(2026, 10), month(2025, 12), month(2026, 8)))
				.when(facts).aggregateByMonth(any(), any(), any(), any(), any(), any());
		StatisticsFilter bySport = new StatisticsFilter(null, UUID.randomUUID(), null, null, null,
				LocalDate.of(2025, 12, 1), LocalDate.of(2026, 10, 31));

		assertThat(new JpaFactBetRepository(facts, dailySums).aggregateByMonth(bySport))
				.extracting(month -> month.year() * 100 + month.month())
				.containsExactly(202512, 202608, 202609, 202610);
	}
}
