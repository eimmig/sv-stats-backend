package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.SearchAggregate;
import com.stakevault.betting.stats.domain.model.SettledBetPoint;
import com.stakevault.betting.stats.domain.model.StatisticsSearchFilter;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaFactBetRepositorySearchIntegrationTest extends TenantSchemaIntegrationSupport {

	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository dimBettingHouseRepository;
	private final DimSportRepository dimSportRepository;
	private final DimLeagueRepository dimLeagueRepository;
	private final DimMarketRepository dimMarketRepository;
	private final DimTeamRepository dimTeamRepository;

	JpaFactBetRepositorySearchIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema,
			JdbcTemplate jdbcTemplate, FactBetRepository factBetRepository, DimDateRepository dimDateRepository,
			DimBettingHouseRepository dimBettingHouseRepository, DimSportRepository dimSportRepository,
			DimLeagueRepository dimLeagueRepository, DimMarketRepository dimMarketRepository,
			DimTeamRepository dimTeamRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.factBetRepository = factBetRepository;
		this.dimDateRepository = dimDateRepository;
		this.dimBettingHouseRepository = dimBettingHouseRepository;
		this.dimSportRepository = dimSportRepository;
		this.dimLeagueRepository = dimLeagueRepository;
		this.dimMarketRepository = dimMarketRepository;
		this.dimTeamRepository = dimTeamRepository;
	}

	@Test
	void shouldMatchTeamIdOnEitherSideAndComputeAvgOddAndOrderedTimeline() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			UUID leagueId = dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID teamA = dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Team A")).id();
			UUID teamB = dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Team B")).id();
			UUID teamC = dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Team C")).id();

			UUID date1 = dimDateRepository.save(new DimDate(UUID.randomUUID(), 1, 9, 2026, 3, "TUESDAY")).id();
			UUID date2 = dimDateRepository.save(new DimDate(UUID.randomUUID(), 5, 9, 2026, 3, "SATURDAY")).id();
			UUID date3 = dimDateRepository.save(new DimDate(UUID.randomUUID(), 10, 9, 2026, 3, "THURSDAY")).id();

			// team A como team1 - WON, odd 1.5, profit 50
			factBetRepository.save(new FactBet(UUID.randomUUID(), date1, houseId, sportId, leagueId, marketId, null,
					teamA, teamB, BigDecimal.valueOf(100), BigDecimal.valueOf(1.5), BigDecimal.valueOf(50), true,
					BetStatus.WON, 1));
			// team A como team2 (visitante) - LOST, odd 2.0, profit -100
			factBetRepository.save(new FactBet(UUID.randomUUID(), date2, houseId, sportId, leagueId, marketId, null,
					teamC, teamA, BigDecimal.valueOf(100), BigDecimal.valueOf(2.0), BigDecimal.valueOf(-100), false,
					BetStatus.LOST, 1));
			// sem team A nos 2 lados - nao deve entrar no filtro por teamId=A
			factBetRepository.save(new FactBet(UUID.randomUUID(), date3, houseId, sportId, leagueId, marketId, null,
					teamC, teamB, BigDecimal.valueOf(100), BigDecimal.valueOf(3.0), BigDecimal.valueOf(200), true,
					BetStatus.WON, 1));

			StatisticsSearchFilter filter = new StatisticsSearchFilter(sportId, leagueId, teamA, null, null, null,
					null, null);

			SearchAggregate aggregate = factBetRepository.aggregateForSearch(filter);
			assertThat(aggregate.settledCount()).isEqualTo(2);
			assertThat(aggregate.totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(200));
			assertThat(aggregate.netProfit()).isEqualByComparingTo(BigDecimal.valueOf(-50));
			assertThat(aggregate.avgOdd()).isEqualByComparingTo(BigDecimal.valueOf(1.75));

			List<SettledBetPoint> timeline = factBetRepository.findOrderedSettledProfits(filter);
			assertThat(timeline).hasSize(2);
			assertThat(timeline.get(0).date()).isEqualTo(LocalDate.of(2026, 9, 1));
			assertThat(timeline.get(0).profit()).isEqualByComparingTo(BigDecimal.valueOf(50));
			assertThat(timeline.get(1).date()).isEqualTo(LocalDate.of(2026, 9, 5));
			assertThat(timeline.get(1).profit()).isEqualByComparingTo(BigDecimal.valueOf(-100));
		}
	}
}
