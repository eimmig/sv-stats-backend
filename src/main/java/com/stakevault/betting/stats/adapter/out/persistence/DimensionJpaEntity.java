package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@NoArgsConstructor
public abstract class DimensionJpaEntity extends AbstractJpaEntity {

	@Column(nullable = false)
	private String name;

	protected DimensionJpaEntity(UUID id, String name) {
		super(id);
		this.name = name;
	}
}
