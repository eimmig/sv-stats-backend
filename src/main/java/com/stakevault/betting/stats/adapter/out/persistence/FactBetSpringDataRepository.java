package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface FactBetSpringDataRepository extends JpaRepository<FactBetJpaEntity, UUID> {
}
