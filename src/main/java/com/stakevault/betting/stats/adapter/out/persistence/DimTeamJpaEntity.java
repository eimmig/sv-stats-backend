package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_team")
@Getter
@NoArgsConstructor
public class DimTeamJpaEntity extends DimensionJpaEntity {

	@Column(name = "sport_id", nullable = false)
	private UUID sportId;

	public DimTeamJpaEntity(UUID id, String name, UUID sportId) {
		super(id, name);
		this.sportId = sportId;
	}
}
