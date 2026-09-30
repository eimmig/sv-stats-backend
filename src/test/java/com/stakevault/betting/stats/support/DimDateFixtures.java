package com.stakevault.betting.stats.support;

import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;

public final class DimDateFixtures {

	private DimDateFixtures() {
	}

	public static UUID ensure(DimDateRepository repository, int day, int month, int year, int quarter,
			String dayOfWeek) {
		repository.insertIfAbsent(new DimDate(UUID.randomUUID(), day, month, year, quarter, dayOfWeek));
		return repository.findByDayAndMonthAndYear(day, month, year).orElseThrow().id();
	}
}
