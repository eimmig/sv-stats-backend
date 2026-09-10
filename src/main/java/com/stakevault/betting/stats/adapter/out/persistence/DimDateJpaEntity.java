package com.stakevault.betting.stats.adapter.out.persistence;

import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "dim_date")
@Getter
@NoArgsConstructor
public class DimDateJpaEntity extends AbstractJpaEntity {

	@Column(nullable = false)
	private int day;

	@Column(nullable = false)
	private int month;

	@Column(nullable = false)
	private int year;

	@Column(nullable = false)
	private int quarter;

	@Column(name = "day_of_week", nullable = false)
	private String dayOfWeek;

	public DimDateJpaEntity(UUID id, int day, int month, int year, int quarter, String dayOfWeek) {
		super(id);
		this.day = day;
		this.month = month;
		this.year = year;
		this.quarter = quarter;
		this.dayOfWeek = dayOfWeek;
	}
}
