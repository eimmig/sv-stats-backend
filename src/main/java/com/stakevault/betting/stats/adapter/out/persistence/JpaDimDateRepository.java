package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;

@Repository
public class JpaDimDateRepository implements DimDateRepository {

	private final DimDateSpringDataRepository jpaRepository;

	public JpaDimDateRepository(DimDateSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public DimDate save(DimDate dimDate) {
		var saved = jpaRepository.save(new DimDateJpaEntity(dimDate.id(), dimDate.day(), dimDate.month(),
				dimDate.year(), dimDate.quarter(), dimDate.dayOfWeek()));
		return toDomain(saved);
	}

	@Override
	public Optional<DimDate> findByDayAndMonthAndYear(int day, int month, int year) {
		return jpaRepository.findByDayAndMonthAndYear(day, month, year).map(JpaDimDateRepository::toDomain);
	}

	private static DimDate toDomain(DimDateJpaEntity entity) {
		return new DimDate(entity.getId(), entity.getDay(), entity.getMonth(), entity.getYear(),
				entity.getQuarter(), entity.getDayOfWeek());
	}
}
