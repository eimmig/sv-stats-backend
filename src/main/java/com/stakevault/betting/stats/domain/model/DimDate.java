package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

public record DimDate(UUID id, int day, int month, int year, int quarter, String dayOfWeek) {
}
