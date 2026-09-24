package com.stakevault.betting.stats.domain.port.out;

import java.util.Optional;

import com.stakevault.betting.stats.domain.model.DimDate;

public interface DimDateRepository {

	DimDate save(DimDate dimDate);

	Optional<DimDate> findByDayAndMonthAndYear(int day, int month, int year);
}
