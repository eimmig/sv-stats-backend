package com.stakevault.betting.stats.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
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
			UUID team1Id = dimensionResolver.resolveTeam(event.team1(), sportId);
			UUID team2Id = dimensionResolver.resolveTeam(event.team2(), sportId);

			// Sem evict aqui: RN06 exclui status=pending de toda agregacao, entao este insert e
			// invisivel para as metricas cacheadas - invalidar agora seria desperdicio (mesmo
			// valor antes/depois). So processSettled muda o que as queries RN06 realmente veem.
			factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
					tipsterId, team1Id, team2Id, event.stake(), event.odd(), null, null, BetStatus.PENDING,
					event.betType(), 1));
		}

		markProcessed(eventId);
	}

	@Override
	@Transactional
	public void processSettled(UUID eventId, BetSettledEvent event) {
		if (processedEventRepository.existsByEventId(eventId)) {
			return;
		}

		Optional<FactBet> existing = factBetRepository.findById(event.betId());
		// dateId reflete a data do JOGO (betDate, resolvida por processCreated a partir de
		// event.betDate() - nao a de registro nem a de liquidacao, decisao do usuario). Achado
		// real do plan review de epic-011: preserva o dateId ja gravado em vez de recalcular a
		// partir de settledAt, que corrompia silenciosamente o agregado mensal do dashboard
		// (feat-006) toda vez que uma aposta liquidava em mes diferente do jogo. Residual aceito:
		// se BetSettled chegar antes do BetCreated correspondente (mensagens fora de ordem), nao
		// ha betDate disponivel neste payload - cai em settledAt como estimativa ate BetCreated
		// processar depois (ver docs/STATISTICS.md "Drawdown maximo").
		UUID dateId = existing.map(FactBet::dateId).orElseGet(() -> dimensionResolver.resolveDate(event.settledAt()));
		// BetSettled nao carrega betType (so BetCreated tem esse campo) - preserva o valor ja
		// gravado no insert em vez de perde-lo a cada liquidacao (mesmo padrao de dateId acima).
		BetType betType = existing.map(FactBet::betType).orElse(null);
		UUID bettingHouseId = dimensionResolver.resolveBettingHouse(event.bettingHouseId(), event.bettingHouseName());
		UUID sportId = dimensionResolver.resolveSport(event.sportId(), event.sportName());
		UUID leagueId = dimensionResolver.resolveLeague(event.leagueId(), event.leagueName());
		UUID marketId = dimensionResolver.resolveMarket(event.marketId(), event.marketName());
		UUID tipsterId = dimensionResolver.resolveTipster(event.tipsterId(), event.tipsterName());
		UUID team1Id = dimensionResolver.resolveTeam(event.team1(), sportId);
		UUID team2Id = dimensionResolver.resolveTeam(event.team2(), sportId);

		factBetRepository.save(new FactBet(event.betId(), dateId, bettingHouseId, sportId, leagueId, marketId,
				tipsterId, team1Id, team2Id, event.stake(), event.odd(), event.profit(),
				event.status() == BetStatus.WON, event.status(), betType, 1));
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
