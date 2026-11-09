package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.port.out.DailyAggregateRepository;

@Repository
public class JpaDailyAggregateRepository implements DailyAggregateRepository {

	private final DailySegmentAggregateSpringDataRepository dailySums;

	public JpaDailyAggregateRepository(DailySegmentAggregateSpringDataRepository dailySums) {
		this.dailySums = dailySums;
	}

	@Override
	public boolean tryLock() {
		return dailySums.tryLockSchema();
	}

	@Override
	public List<LocalDate> takeDirtyDays() {
		return dailySums.takeDirtyDays();
	}

	@Override
	public void rebuild(Collection<LocalDate> days) {
		dailySums.deleteDays(days);
		dailySums.insertDays(days);
	}
}
