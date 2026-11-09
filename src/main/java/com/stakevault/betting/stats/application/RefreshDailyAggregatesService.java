package com.stakevault.betting.stats.application;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stakevault.betting.stats.domain.port.in.RefreshDailyAggregatesUseCase;
import com.stakevault.betting.stats.domain.port.out.DailyAggregateRepository;

@Service
public class RefreshDailyAggregatesService implements RefreshDailyAggregatesUseCase {

	private final DailyAggregateRepository dailyAggregateRepository;

	public RefreshDailyAggregatesService(DailyAggregateRepository dailyAggregateRepository) {
		this.dailyAggregateRepository = dailyAggregateRepository;
	}

	@Override
	@Transactional
	public int refreshDirtyDays(int maxDays) {
		if (!dailyAggregateRepository.tryLock()) {
			return 0;
		}
		List<LocalDate> days = dailyAggregateRepository.takeDirtyDays(maxDays);
		if (!days.isEmpty()) {
			dailyAggregateRepository.rebuild(days);
		}
		return days.size();
	}
}
