package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.stakevault.betting.stats.domain.model.ProcessedEvent;
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;

@Repository
public class JpaProcessedEventRepository implements ProcessedEventRepository {

	private final ProcessedEventSpringDataRepository jpaRepository;

	public JpaProcessedEventRepository(ProcessedEventSpringDataRepository jpaRepository) {
		this.jpaRepository = jpaRepository;
	}

	@Override
	public ProcessedEvent save(ProcessedEvent processedEvent) {
		var saved = jpaRepository.save(new ProcessedEventJpaEntity(processedEvent));
		return new ProcessedEvent(saved.getId(), saved.getEventId(), saved.getProcessedAt());
	}

	@Override
	public boolean existsByEventId(UUID eventId) {
		return jpaRepository.existsByEventId(eventId);
	}
}
