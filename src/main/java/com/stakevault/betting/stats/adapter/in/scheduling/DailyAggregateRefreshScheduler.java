package com.stakevault.betting.stats.adapter.in.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
 *
 * <p>Intervalo e limite de dias por rodada (feat-037): com 5 s e sem limite, uma carga que tocava os
 * 366 dias do ano fazia cada rodada recalcular tudo (5,5 s no banco de 500m), e o recalculo tirava
 * metade do processador do consumo de eventos. O intervalo nao muda resultado, porque dia pendente
 * e lido de fact_bet; so muda quando as somas voltam a ser usadas.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "stats.daily-aggregates.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class DailyAggregateRefreshScheduler {

	private static final Logger log = LoggerFactory.getLogger(DailyAggregateRefreshScheduler.class);

	private final TenantSchemaGateway tenantSchemaGateway;
	private final RefreshDailyAggregatesUseCase refreshDailyAggregates;
	private final int maxDaysPerRefresh;

	public DailyAggregateRefreshScheduler(TenantSchemaGateway tenantSchemaGateway,
			RefreshDailyAggregatesUseCase refreshDailyAggregates,
			@Value("${stats.daily-aggregates.max-days-per-refresh:31}") int maxDaysPerRefresh) {
		this.tenantSchemaGateway = tenantSchemaGateway;
		this.refreshDailyAggregates = refreshDailyAggregates;
		this.maxDaysPerRefresh = maxDaysPerRefresh;
	}

	@Scheduled(fixedDelayString = "${stats.daily-aggregates.refresh-delay-ms:60000}")
	public void refreshAll() {
		for (TenantSchemaName schema : tenantSchemaGateway.listWithDailyAggregates()) {
			try (var _ = TenantContextScope.open(schema)) {
				refreshDailyAggregates.refreshDirtyDays(maxDaysPerRefresh);
			} catch (RuntimeException e) {
				log.warn("daily aggregate refresh failed for {}", schema.value(), e);
			}
		}
	}
}
