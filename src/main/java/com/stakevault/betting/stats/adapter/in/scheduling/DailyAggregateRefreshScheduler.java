package com.stakevault.betting.stats.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.in.RefreshDailyAggregatesUseCase;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;

/**
 * Recalcula, a cada rodada, os dias pendentes de cada organizacao (feat-036). Roda em todas as
 * replicas; o lock por schema em RefreshDailyAggregatesService deixa uma so recalcular. Falha em
 * uma organizacao nao impede as demais, e os dias dela continuam pendentes.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "stats.daily-aggregates.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class DailyAggregateRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(DailyAggregateRefreshScheduler.class);

	private final TenantSchemaGateway tenantSchemaGateway;
	private final RefreshDailyAggregatesUseCase refreshDailyAggregates;

	public DailyAggregateRefreshScheduler(TenantSchemaGateway tenantSchemaGateway,
			RefreshDailyAggregatesUseCase refreshDailyAggregates) {
		this.tenantSchemaGateway = tenantSchemaGateway;
		this.refreshDailyAggregates = refreshDailyAggregates;
	}

	@Scheduled(fixedDelayString = "${stats.daily-aggregates.refresh-delay-ms:5000}")
	public void refreshAll() {
		for (TenantSchemaName schema : tenantSchemaGateway.listWithDailyAggregates()) {
			try (var _ = TenantContextScope.open(schema)) {
				refreshDailyAggregates.refreshDirtyDays();
			} catch (RuntimeException e) {
				log.warn("daily aggregate refresh failed for {}", schema.value(), e);
			}
		}
	}
}
