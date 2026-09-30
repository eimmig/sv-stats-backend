package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface DimBettingHouseSpringDataRepository extends JpaRepository<DimBettingHouseJpaEntity, UUID> {

	@Transactional
	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO dim_betting_house (id, name) VALUES (:id, :name) ON CONFLICT DO NOTHING", nativeQuery = true)
	void insertIfAbsent(@Param("id") UUID id, @Param("name") String name);
}
