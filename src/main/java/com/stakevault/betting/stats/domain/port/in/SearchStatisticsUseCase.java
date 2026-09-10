package com.stakevault.betting.stats.domain.port.in;

import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;
import com.stakevault.betting.stats.domain.model.StatisticsSearchResult;

public interface SearchStatisticsUseCase {

	StatisticsSearchResult search(StatisticsSearchFilter filter);
}
