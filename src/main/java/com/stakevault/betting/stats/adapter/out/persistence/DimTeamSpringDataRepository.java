package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface DimTeamSpringDataRepository extends JpaRepository<DimTeamJpaEntity, UUID> {

	Optional<DimTeamJpaEntity> findByNameAndSportId(String name, UUID sportId);

	List<DimTeamJpaEntity> findBySportIdOrderByName(UUID sportId);

	@Transactional
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO dim_team (id, name, sport_id) VALUES (:id, :name, :sportId) ON CONFLICT DO NOTHING", nativeQuery = true)
	void insertIfAbsent(@Param("id") UUID id, @Param("name") String name, @Param("sportId") UUID sportId);
}
