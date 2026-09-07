package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_betting_house")
@NoArgsConstructor
public class DimBettingHouseJpaEntity extends DimensionJpaEntity {

	public DimBettingHouseJpaEntity(UUID id, String name) {
		super(id, name);
	}
}
