package com.stakevault.betting.stats.adapter.in.scheduling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.stakevault.betting.stats.config.TenantContextHolder;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.in.RefreshDailyAggregatesUseCase;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;

@ExtendWith(MockitoExtension.class)
class DailyAggregateRefreshSchedulerTest {

	private static final TenantSchemaName FIRST = TenantSchemaName.fromSlug("first-org");
	private static final TenantSchemaName SECOND = TenantSchemaName.fromSlug("second-org");

	@Mock
	private TenantSchemaGateway tenantSchemaGateway;

	@Test
	void shouldRefreshEachTenantInsideItsOwnSchemaAndGoOnAfterAFailure() {
		when(tenantSchemaGateway.listWithDailyAggregates()).thenReturn(List.of(FIRST, SECOND));
		List<TenantSchemaName> seen = new ArrayList<>();
		RefreshDailyAggregatesUseCase refresh = maxDays -> {
			assertThat(maxDays).isEqualTo(31);
			seen.add(TenantContextHolder.current());
			if (seen.size() == 1) {
				throw new IllegalStateException("database unavailable");
			}
			return 1;
		};

		new DailyAggregateRefreshScheduler(tenantSchemaGateway, refresh, 31).refreshAll();

		assertThat(seen).containsExactly(FIRST, SECOND);
		assertThat(TenantContextHolder.current()).isNull();
	}
}
