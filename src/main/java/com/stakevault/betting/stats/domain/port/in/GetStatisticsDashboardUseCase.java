package com.stakevault.betting.stats.domain.port.in;

import com.stakevault.betting.stats.domain.model.StatisticsDashboard;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;

public interface GetStatisticsDashboardUseCase {

	StatisticsDashboard getDashboard(StatisticsFilter filter);
}
