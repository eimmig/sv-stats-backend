package com.stakevault.betting.stats.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;

// Resolve o id de cada dimensao do esquema estrela a partir do payload do evento, criando a
// linha sob demanda na primeira aposta que a referencia (upsert-if-missing) - id e o mesmo uuid
// do catalogo em bets-service para as 5 dimensoes nominais; DimDate/DimTeam sao a excecao,
// localizadas por chave natural (dia/mes/ano; name) porque o evento nao carrega um id proprio
// pra elas (team1/team2 sao texto livre em bets-service, sem catalogo - ver docs/DATA-MODEL.md).
@Service
public class DimensionResolver {

	private final DimBettingHouseRepository bettingHouseRepository;
	private final DimSportRepository sportRepository;
	private final DimLeagueRepository leagueRepository;
	private final DimMarketRepository marketRepository;
	private final DimTipsterRepository tipsterRepository;
	private final DimDateRepository dateRepository;
	private final DimTeamRepository teamRepository;

	public DimensionResolver(DimBettingHouseRepository bettingHouseRepository, DimSportRepository sportRepository,
			DimLeagueRepository leagueRepository, DimMarketRepository marketRepository,
			DimTipsterRepository tipsterRepository, DimDateRepository dateRepository,
			DimTeamRepository teamRepository) {
		this.bettingHouseRepository = bettingHouseRepository;
		this.sportRepository = sportRepository;
		this.leagueRepository = leagueRepository;
		this.marketRepository = marketRepository;
		this.tipsterRepository = tipsterRepository;
		this.dateRepository = dateRepository;
		this.teamRepository = teamRepository;
	}

	public UUID resolveBettingHouse(UUID id, String name) {
		if (!bettingHouseRepository.existsById(id)) {
			bettingHouseRepository.save(new DimBettingHouse(id, name));
		}
		return id;
	}

	public UUID resolveSport(UUID id, String name) {
		if (!sportRepository.existsById(id)) {
			sportRepository.save(new DimSport(id, name));
		}
		return id;
	}

	public UUID resolveLeague(UUID id, String name) {
		if (!leagueRepository.existsById(id)) {
			leagueRepository.save(new DimLeague(id, name));
		}
		return id;
	}

	public UUID resolveMarket(UUID id, String name) {
		if (!marketRepository.existsById(id)) {
			marketRepository.save(new DimMarket(id, name));
		}
		return id;
	}

	public UUID resolveTipster(UUID id, String name) {
		if (id == null) {
			return null;
		}
		if (!tipsterRepository.existsById(id)) {
			tipsterRepository.save(new DimTipster(id, name));
		}
		return id;
	}

	public UUID resolveDate(Instant instant) {
		LocalDate date = instant.atZone(ZoneOffset.UTC).toLocalDate();
		return dateRepository.findByDayAndMonthAndYear(date.getDayOfMonth(), date.getMonthValue(), date.getYear())
				.map(DimDate::id)
				.orElseGet(() -> dateRepository.save(new DimDate(UUID.randomUUID(), date.getDayOfMonth(),
						date.getMonthValue(), date.getYear(), quarterOf(date), date.getDayOfWeek().name())).id());
	}

	// team1/team2 nao tem catalogo em bets-service (texto livre por aposta) - sem id proprio no
	// evento, resolvida por chave natural (name), mesmo padrao de resolveDate. Nem toda aposta
	// referencia os 2 lados (ex. mercado sem confronto de dois lados) - null passa direto.
	public UUID resolveTeam(String name) {
		if (name == null) {
			return null;
		}
		return teamRepository.findByName(name).map(DimTeam::id)
				.orElseGet(() -> teamRepository.save(new DimTeam(UUID.randomUUID(), name)).id());
	}

	private static int quarterOf(LocalDate date) {
		return ((date.getMonthValue() - 1) / 3) + 1;
	}
}
