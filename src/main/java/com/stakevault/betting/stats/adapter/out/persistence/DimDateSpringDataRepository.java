package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DimDateSpringDataRepository extends JpaRepository<DimDateJpaEntity, UUID> {

	Optional<DimDateJpaEntity> findByDayAndMonthAndYear(int day, int month, int year);
}
