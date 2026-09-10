package com.stakevault.betting.stats.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.ProcessedEvent;
import com.stakevault.betting.stats.domain.port.in.BetCreatedEvent;
import com.stakevault.betting.stats.domain.port.in.BetSettledEvent;
import com.stakevault.betting.stats.domain.port.in.ProcessBetEventUseCase;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.domain.port.out.MetricsCacheRepository;
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;

@Service
public class ProcessBetEventService implements ProcessBetEventUseCase {

	private final FactBetRepository factBetRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final DimensionResolver dimensionResolver;
	private final MetricsCacheRepository metricsCacheRepository;

	public ProcessBetEventService(FactBetRepository factBetRepository,
			ProcessedEventRepository processedEventRepository, DimensionResolver dimensionResolver,
			MetricsCacheRepository metricsCacheRepository) {
		this.factBetRepository = factBetRepository;
		this.processedEventRepository = processedEventRepository;
		this.dimensionResolver = dimensionResolver;
		this.metricsCacheRepository = metricsCacheRepository;
	}

	@Override
	@Transactional
	public void processCreated(UUID eventId, BetCreatedEvent event) {
		if (processedEventRepository.existsByEventId(eventId)) {
			return;
		}

		// BetSettled pode ter chegado antes e ja liquidado esta linha (mensagens fora de ordem,
		// ver plan_review) - BetCreated nunca sobrescreve uma liquidacao ja aplicada.
		if (factBetRepository.findById(event.betId()).isEmpty()) {
			UUID dateId = dimensionResolver.resolveDate(event.betDate());
			UUID bettingHouseId = dimensionResolver.resolveBettingHouse(event.bettingHouseId(),
					event.bettingHouseName());
			UUID sportId = dimensionResolver.resolveSport(event.sportId(), event.sportName());
			UUID leagueId = dimensionResolver.resolveLeague(event.leagueId(), event.leagueName());
			UUID marketId = dimensionResolver.resolveMarket(event.marketId(), event.marketName());
			UUID tipsterId = dimensionResolver.resolveTipster(event.tipsterId(), event.tipsterName());

			// team1Id/team2Id/odd: mapeamento do evento chega em feat-012.2 (BetCreatedEvent ainda
			// nao carrega team1/team2/odd) - null por enquanto, sem quebrar o build.
			// Sem evict aqui: RN06 exclui status=pending de toda agregacao, entao este insert e
			// invisivel para as metricas cacheadas - invalidar agora seria desperdicio (mesmo
			// valor antes/depois). So processSettled muda o que as queries RN06 realmente veem.
			factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
					tipsterId, null, null, event.stake(), null, null, null, BetStatus.PENDING, 1));
		}

		markProcessed(eventId);
	}

	@Override
	@Transactional
	public void processSettled(UUID eventId, BetSettledEvent event) {
		if (processedEventRepository.existsByEventId(eventId)) {
			return;
		}

		UUID dateId = dimensionResolver.resolveDate(event.settledAt());
		UUID bettingHouseId = dimensionResolver.resolveBettingHouse(event.bettingHouseId(), event.bettingHouseName());
		UUID sportId = dimensionResolver.resolveSport(event.sportId(), event.sportName());
		UUID leagueId = dimensionResolver.resolveLeague(event.leagueId(), event.leagueName());
		UUID marketId = dimensionResolver.resolveMarket(event.marketId(), event.marketName());
		UUID tipsterId = dimensionResolver.resolveTipster(event.tipsterId(), event.tipsterName());

		// team1Id/team2Id/odd: mapeamento do evento chega em feat-012.2 (BetSettledEvent ainda nao
		// carrega team1/team2/odd) - null por enquanto, sem quebrar o build.
		factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
				tipsterId, null, null, event.stake(), null, event.profit(), event.status() == BetStatus.WON,
				event.status(), 1));
		evictMetricsFor(event.settledAt());

		markProcessed(eventId);
	}

	private void evictMetricsFor(Instant instant) {
		LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
		metricsCacheRepository.evict(date.getYear(), date.getMonthValue());
	}

	private void markProcessed(UUID eventId) {
		processedEventRepository.save(new ProcessedEvent(UUID.randomUUID(), eventId, Instant.now()));
	}
}
