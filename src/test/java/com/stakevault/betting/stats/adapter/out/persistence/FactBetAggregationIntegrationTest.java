package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetAggregate;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimDate;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
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
		return newDateId(6, 9, 2026);
	}

	private UUID newDateId(int day, int month, int year) {
		return dimDateRepository.save(new DimDate(UUID.randomUUID(), day, month, year, (month - 1) / 3 + 1,
				"SUNDAY"))
				.id();
	}

	private UUID newLeagueId() {
		return dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
	}

	private FactBet settledBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake, BigDecimal profit,
			boolean isWin) {
		return settledBet(newDateId(), bettingHouseId, sportId, marketId, stake, profit, isWin);
	}

	private FactBet settledBet(UUID dateId, UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake,
			BigDecimal profit, boolean isWin) {
		return new FactBet(UUID.randomUUID(), dateId, bettingHouseId, sportId, newLeagueId(), marketId, null, null,
				null, stake, null, profit, isWin, isWin ? BetStatus.WON : BetStatus.LOST, null, 1);
	}

	private FactBet pendingBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				null, null, stake, null, null, null, BetStatus.PENDING, null, 1);
	}

	// RN06 inclui explicitamente void nas agregacoes (aposta devolvida) - stake volta pro
	// apostador, profit=0, nao conta como vitoria nem derrota.
	private FactBet voidBet(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				null, null, stake, null, BigDecimal.ZERO, false, BetStatus.VOID, null, 1);
	}

	private FactBet settledBetWithTypeAndOdd(UUID bettingHouseId, UUID sportId, UUID marketId, BigDecimal stake,
			BigDecimal odd, BigDecimal profit, BetStatus status, BetType betType) {
		return new FactBet(UUID.randomUUID(), newDateId(), bettingHouseId, sportId, newLeagueId(), marketId, null,
				null, null, stake, odd, profit, status == BetStatus.WON, status, betType, 1);
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

			BetAggregate aggregate = factBetRepository.aggregateOverall(StatisticsFilter.none());

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
			BetAggregate aggregate = factBetRepository.aggregateOverall(StatisticsFilter.none());

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

			var bySport = factBetRepository.aggregateBySport(StatisticsFilter.none());

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

			var byMarket = factBetRepository.aggregateByMarket(StatisticsFilter.none());

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

			var byHouse = factBetRepository.aggregateByBettingHouse(StatisticsFilter.none());

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

	@Test
	void shouldExcludeOtherSportsWhenFilteredBySportId() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID soccerId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			UUID tennisId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Tennis")).id();
			factBetRepository.save(
					settledBet(houseId, soccerId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(50), true));
			factBetRepository.save(
					settledBet(houseId, tennisId, marketId, BigDecimal.valueOf(100), BigDecimal.valueOf(-100), false));

			BetAggregate aggregate = factBetRepository
					.aggregateOverall(new StatisticsFilter(null, soccerId, null, null, null, null, null));

			assertThat(aggregate.totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(100));
			assertThat(aggregate.settledCount()).isEqualTo(1);
			assertThat(aggregate.wonCount()).isEqualTo(1);
		}
	}

	// epic-014: lostCount/voidCount/preCount/liveCount/avgOdd - campos novos sobre o mesmo
	// agregado. wonCount continua vindo de isWin (nao duplicado via status = :won).
	@Test
	void shouldComputeLostVoidPreLiveAndAvgOddOnOverallAggregate() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			factBetRepository.save(settledBetWithTypeAndOdd(houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(2.0), BigDecimal.valueOf(100), BetStatus.WON, BetType.PRE));
			factBetRepository.save(settledBetWithTypeAndOdd(houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(1.5), BigDecimal.valueOf(-100), BetStatus.LOST, BetType.LIVE));
			factBetRepository.save(settledBetWithTypeAndOdd(houseId, sportId, marketId, BigDecimal.valueOf(100), null,
					BigDecimal.ZERO, BetStatus.VOID, null));

			BetAggregate aggregate = factBetRepository.aggregateOverall(StatisticsFilter.none());

			assertThat(aggregate.wonCount()).isEqualTo(1);
			assertThat(aggregate.lostCount()).isEqualTo(1);
			assertThat(aggregate.voidCount()).isEqualTo(1);
			assertThat(aggregate.preCount()).isEqualTo(1);
			assertThat(aggregate.liveCount()).isEqualTo(1);
			assertThat(aggregate.settledCount()).isEqualTo(3);
			// AVG ignora null (so 2 das 3 apostas tem odd) - (2.0 + 1.5) / 2 = 1.75.
			assertThat(aggregate.avgOdd()).isEqualByComparingTo(BigDecimal.valueOf(1.75));
		}
	}

	@Test
	void shouldReturnNullAvgOddWhenNoSettledBetHasOdd() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			factBetRepository.save(settledBet(houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(50), true));

			BetAggregate aggregate = factBetRepository.aggregateOverall(StatisticsFilter.none());

			assertThat(aggregate.avgOdd()).isNull();
			assertThat(aggregate.preCount()).isZero();
			assertThat(aggregate.liveCount()).isZero();
		}
	}

	@Test
	void shouldExcludeBetsOutsideTheFromToDateRange() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID houseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
			UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
			UUID januaryDate = newDateId(15, 1, 2026);
			UUID marchDate = newDateId(15, 3, 2026);
			factBetRepository.save(settledBet(januaryDate, houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(50), true));
			factBetRepository.save(settledBet(marchDate, houseId, sportId, marketId, BigDecimal.valueOf(100),
					BigDecimal.valueOf(-100), false));

			StatisticsFilter januaryOnly = new StatisticsFilter(null, null, null, null, null,
					java.time.LocalDate.of(2026, 1, 1), java.time.LocalDate.of(2026, 1, 31));
			BetAggregate aggregate = factBetRepository.aggregateOverall(januaryOnly);

			assertThat(aggregate.totalStaked()).isEqualByComparingTo(BigDecimal.valueOf(100));
			assertThat(aggregate.settledCount()).isEqualTo(1);
			assertThat(aggregate.wonCount()).isEqualTo(1);
		}
	}
}
