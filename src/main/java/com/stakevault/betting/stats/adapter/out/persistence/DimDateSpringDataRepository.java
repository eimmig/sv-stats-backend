package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface DimDateSpringDataRepository extends JpaRepository<DimDateJpaEntity, UUID> {

	Optional<DimDateJpaEntity> findByDayAndMonthAndYear(int day, int month, int year);

	@Modifying(flushAutomatically = true)
	@Query(value = "INSERT INTO dim_date (id, day, month, year, quarter, day_of_week) VALUES (:id, :day, :month, :year, :quarter, :dayOfWeek) ON CONFLICT DO NOTHING", nativeQuery = true)
	void insertIfAbsent(@Param("id") UUID id, @Param("day") int day, @Param("month") int month,
			@Param("year") int year, @Param("quarter") int quarter, @Param("dayOfWeek") String dayOfWeek);
}
