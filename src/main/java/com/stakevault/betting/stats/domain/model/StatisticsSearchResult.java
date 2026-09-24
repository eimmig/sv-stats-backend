package com.stakevault.betting.stats.domain.model;

import java.util.List;

public record StatisticsSearchResult(StatisticsSearchFilter filters, StatisticsSearchSummary summary,
		List<TimelinePoint> timeline) {
}
