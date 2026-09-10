package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_team")
@NoArgsConstructor
public class DimTeamJpaEntity extends DimensionJpaEntity {

	public DimTeamJpaEntity(UUID id, String name) {
		super(id, name);
	}
}
