package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_market")
@NoArgsConstructor
public class DimMarketJpaEntity extends DimensionJpaEntity {

	public DimMarketJpaEntity(UUID id, String name) {
		super(id, name);
	}
}
