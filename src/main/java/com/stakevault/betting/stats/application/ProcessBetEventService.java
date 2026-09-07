package com.stakevault.betting.stats.application;

import java.time.Instant;
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
import com.stakevault.betting.stats.domain.port.out.ProcessedEventRepository;

@Service
public class ProcessBetEventService implements ProcessBetEventUseCase {

	private final FactBetRepository factBetRepository;
	private final ProcessedEventRepository processedEventRepository;
	private final DimensionResolver dimensionResolver;

	public ProcessBetEventService(FactBetRepository factBetRepository,
			ProcessedEventRepository processedEventRepository, DimensionResolver dimensionResolver) {
		this.factBetRepository = factBetRepository;
		this.processedEventRepository = processedEventRepository;
		this.dimensionResolver = dimensionResolver;
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

			factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
					tipsterId, event.stake(), null, null, BetStatus.PENDING, 1));
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

		factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
				tipsterId, event.stake(), event.profit(), event.status() == BetStatus.WON, event.status(), 1));

		markProcessed(eventId);
	}

	private void markProcessed(UUID eventId) {
		processedEventRepository.save(new ProcessedEvent(UUID.randomUUID(), eventId, Instant.now()));
	}
}
