package com.stakevault.betting.stats.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.ProcessedEvent;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "processed_event")
@Getter
@NoArgsConstructor
public class ProcessedEventJpaEntity extends AbstractJpaEntity {

	@Column(name = "event_id", nullable = false)
	private UUID eventId;

	@Column(name = "processed_at", nullable = false)
	private Instant processedAt;

	public ProcessedEventJpaEntity(ProcessedEvent processedEvent) {
		super(processedEvent.id());
		this.eventId = processedEvent.eventId();
		this.processedAt = processedEvent.processedAt();
	}
}
