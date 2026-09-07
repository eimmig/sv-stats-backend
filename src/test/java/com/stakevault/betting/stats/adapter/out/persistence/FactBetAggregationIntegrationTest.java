package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class FactBetAggregationIntegrationTest extends TenantSchemaIntegrationSupport {

	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository dimBettingHouseRepository;
	private final DimSportRepository dimSportRepository;
	private final DimLeagueRepository dimLeagueRepository;
	private final DimMarketRepository dimMarketRepository;

	FactBetAggregationIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			FactBetRepository factBetRepository, DimDateRepository dimDateRepository,
			DimBettingHouseRepository dimBettingHouseRepository, DimSportRepository dimSportRepository,
			DimLeagueRepository dimLeagueRepository, DimMarketRepository dimMarketRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.factBetRepository = factBetRepository;
		this.dimDateRepository = dimDateRepository;
		this.dimBettingHouseRepository = dimBettingHouseRepository;
		this.dimSportRepository = dimSportRepository;
		this.dimLeagueRepository = dimLeagueRepository;
		this.dimMarketRepository = dimMarketRepository;
	}

	private UUID newDateId() {
		return dimDateRepository.save(new DimDate(UUID.randomUUID(), 6, 9, 2026, 3, "SUNDAY")).id();
	}

	private UUID newLeagueId() {
		return dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
	}

	private FactBet settledBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake, BigDecimal profit,
			boolean isWin) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				stake, profit, isWin, isWin ? BetStatus.WON : BetStatus.LOST, 1);
	}

	private FactBet pendingBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				stake, null, null, BetStatus.PENDING, 1);
	}

	// RN06 inclui explicitamente void nas agregacoes (aposta devolvida) - stake volta pro
	// apostador, profit=0, nao conta como vitoria nem derrota.
	private FactBet voidBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				stake, BigDecimal.ZERO, false, BetStatus.VOID, 1);
	}

	@Test
	void shouldExcludePendingBetsFromOverallAggregate() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			factBetRepository.save(settledBet(houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(50), true));
			factBetRepository.save(settledBet(houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(-100), false));
			factBetRepository.save(voidBet(houseId, sportId, marketId, BigDecimal.valueOf(100)));
			factBetRepository.save(pendingBet(houseId, sportId, marketId, BigDecimal.valueOf(999)));

			BetAggregate aggregate = factBetRepository.aggregateOverall();

			// void entra na soma (RN06) mas nao conta como vitoria.
			assertThat(aggregate.totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(300));
			assertThat(aggregate.netProfit()).isEqualByComparingTo(BigDecimal.valueOf(-50));
			assertThat(aggregate.wonCount()).isEqualTo(1);
			assertThat(aggregate.settledCount()).isEqualTo(3);
		}
	}

	@Test
	void shouldReturnZeroedAggregateWhenNoSettledBetExists() {
		try (var _ = TenantContextScope.open(schema)) {
			BetAggregate aggregate = factBetRepository.aggregateOverall();

			assertThat(aggregate.totalStaked()).isEqualByComparingTo(BigDecimal.ZERO);
			assertThat(aggregate.netProfit()).isEqualByComparingTo(BigDecimal.ZERO);
			assertThat(aggregate.wonCount()).isZero();
			assertThat(aggregate.settledCount()).isZero();
		}
	}

	@Test
	void shouldAggregateBySportConsideringOnlyItsOwnBets() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID soccerId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			UUID tennisId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Tennis")).id();
			factBetRepository.save(
					settledBet(houseId, soccerId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(50), true));
			factBetRepository.save(
					settledBet(houseId, soccerId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(50), true));
			factBetRepository.save(
					settledBet(houseId, tennisId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(-100), false));
			factBetRepository.save(pendingBet(houseId, tennisId, marketId, BigDecimal.valueOf(999)));

			var bySport = factBetRepository.aggregateBySport();

			SegmentedBetAggregate soccer = bySport.stream()
					.filter(segment -> segment.dimensionId().equals(soccerId))
					.findFirst()
					.orElseThrow();
			assertThat(soccer.dimensionName()).isEqualTo("Soccer");
			assertThat(soccer.aggregate().totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(200));
			assertThat(soccer.aggregate().netProfit()).isEqualByComparingTo(BigDecimal.valueOf(100));
			assertThat(soccer.aggregate().wonCount()).isEqualTo(2);
			assertThat(soccer.aggregate().settledCount()).isEqualTo(2);

			SegmentedBetAggregate tennis = bySport.stream()
					.filter(segment -> segment.dimensionId().equals(tennisId))
					.findFirst()
					.orElseThrow();
			assertThat(tennis.dimensionName()).isEqualTo("Tennis");
			assertThat(tennis.aggregate().totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(100));
			assertThat(tennis.aggregate().settledCount()).isEqualTo(1);
		}
	}

	@Test
	void shouldAggregateByMarket() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Over/Under")).id();
			factBetRepository.save(
					settledBet(houseId, sportId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(50), true));

			var byMarket = factBetRepository.aggregateByMarket();

			SegmentedBetAggregate segment = byMarket.stream()
					.filter(s -> s.dimensionId().equals(marketId))
					.findFirst()
					.orElseThrow();
			assertThat(segment.dimensionName()).isEqualTo("Over/Under");
			assertThat(segment.aggregate().totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(100));
			assertThat(segment.aggregate().wonCount()).isEqualTo(1);
		}
	}

	@Test
	void shouldAggregateByBettingHouse() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "Bet365")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			factBetRepository.save(
					settledBet(houseId, sportId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(-100), false));

			var byHouse = factBetRepository.aggregateByBettingHouse();

			SegmentedBetAggregate segment = byHouse.stream()
					.filter(s -> s.dimensionId().equals(houseId))
					.findFirst()
					.orElseThrow();
			assertThat(segment.dimensionName()).isEqualTo("Bet365");
			assertThat(segment.aggregate().netProfit()).isEqualByComparingTo(BigDecimal.valueOf(-100));
			assertThat(segment.aggregate().wonCount()).isZero();
			assertThat(segment.aggregate().settledCount()).isEqualTo(1);
		}
	}
}
