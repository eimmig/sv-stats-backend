package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DimSportSpringDataRepository extends JpaRepository<DimSportJpaEntity, UUID> {
}
