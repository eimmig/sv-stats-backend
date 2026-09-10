package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface DimTeamSpringDataRepository extends JpaRepository<DimTeamJpaEntity, UUID> {

	Optional<DimTeamJpaEntity> findByNameAndSportId(String name, UUID sportId);

	List<DimTeamJpaEntity> findBySportIdOrderByName(UUID sportId);
}
