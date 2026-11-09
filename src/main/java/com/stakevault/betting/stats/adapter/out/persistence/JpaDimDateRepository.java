package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;

@Repository
public class JpaDimDateRepository implements DimDateRepository {

	private final DimDateSpringDataRepository jpaRepository;
	private final KnownDimensions knownDimensions;

	public JpaDimDateRepository(DimDateSpringDataRepository jpaRepository, KnownDimensions knownDimensions) {
		this.jpaRepository = jpaRepository;
		this.knownDimensions = knownDimensions;
	}

	@Override
	public DimDate save(DimDate dimDate) {
		var saved = jpaRepository.save(new DimDateJpaEntity(dimDate.id(), dimDate.day(), dimDate.month(),
				dimDate.year(), dimDate.quarter(), dimDate.dayOfWeek()));
		return toDomain(saved);
	}

	@Override
	public void insertIfAbsent(DimDate dimDate) {
		jpaRepository.insertIfAbsent(dimDate.id(), dimDate.day(), dimDate.month(), dimDate.year(),
				dimDate.quarter(), dimDate.dayOfWeek());
	}

	@Override
	public Optional<DimDate> findByDayAndMonthAndYear(int day, int month, int year) {
		String key = year + "-" + month + "-" + day;
		Optional<DimDate> known = knownDimensions.find("date", key, DimDate.class);
		if (known.isPresent()) {
			return known;
		}
		Optional<DimDate> found = jpaRepository.findByDayAndMonthAndYear(day, month, year)
				.map(JpaDimDateRepository::toDomain);
		found.ifPresent(date -> knownDimensions.remember("date", key, date));
		return found;
	}

	private static DimDate toDomain(DimDateJpaEntity entity) {
		return new DimDate(entity.getId(), entity.getDay(), entity.getMonth(), entity.getYear(),
				entity.getQuarter(), entity.getDayOfWeek());
	}
}
