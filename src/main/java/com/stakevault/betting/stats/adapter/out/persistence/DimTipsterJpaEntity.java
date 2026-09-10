package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_tipster")
@NoArgsConstructor
public class DimTipsterJpaEntity extends DimensionJpaEntity {

	public DimTipsterJpaEntity(UUID id, String name) {
		super(id, name);
	}
}
