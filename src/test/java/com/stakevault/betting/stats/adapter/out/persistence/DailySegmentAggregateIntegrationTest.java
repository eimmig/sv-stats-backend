package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.assertj.core.api.recursive.comparison.RecursiveComparisonConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.DimBettingHouse;
import com.stakevault.betting.stats.domain.model.DimLeague;
import com.stakevault.betting.stats.domain.model.DimMarket;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.model.DimTipster;
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.SegmentedBetAggregate;
import com.stakevault.betting.stats.domain.model.StatisticsFilter;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.in.RefreshDailyAggregatesUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.domain.port.out.DimTipsterRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.domain.port.out.TenantSchemaGateway;
import com.stakevault.betting.stats.support.DimDateFixtures;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

/**
 * As somas diarias (feat-036) tem de dar o mesmo resultado que fact_bet. Com dia pendente no
 * intervalo, JpaFactBetRepository le fact_bet; depois do recalculo, le agg_daily_segment. O teste
 * compara as nove agregacoes do dashboard nos dois momentos, com casos que as regras tratam a parte:
 * pendente, devolvida, odd nula, sem tipster, sem tipo de aposta e time repetido ou ausente.
 */
class DailySegmentAggregateIntegrationTest extends TenantSchemaIntegrationSupport {

	private static final LocalDate DAY_1 = LocalDate.of(2026, 10, 1);
	private static final LocalDate DAY_2 = LocalDate.of(2026, 10, 2);
	private static final LocalDate DAY_3 = LocalDate.of(2026, 10, 3);
	private static final StatisticsFilter PERIOD = new StatisticsFilter(null, null, null, null, null, DAY_1, DAY_3);

	private final FactBetRepository factBetRepository;
	private final RefreshDailyAggregatesUseCase refreshDailyAggregates;
	private final TenantSchemaGateway tenantSchemaGateway;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository houses;
	private final DimSportRepository sports;
	private final DimLeagueRepository leagues;
	private final DimMarketRepository markets;
	private final DimTipsterRepository tipsters;
	private final DimTeamRepository teams;

	DailySegmentAggregateIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			FactBetRepository factBetRepository, RefreshDailyAggregatesUseCase refreshDailyAggregates,
			TenantSchemaGateway tenantSchemaGateway, DimDateRepository dimDateRepository,
			DimBettingHouseRepository houses, DimSportRepository sports, DimLeagueRepository leagues,
			DimMarketRepository markets, DimTipsterRepository tipsters, DimTeamRepository teams) {
		super(provisionTenantSchema, jdbcTemplate);
		this.factBetRepository = factBetRepository;
		this.refreshDailyAggregates = refreshDailyAggregates;
		this.tenantSchemaGateway = tenantSchemaGateway;
		this.dimDateRepository = dimDateRepository;
		this.houses = houses;
		this.sports = sports;
		this.leagues = leagues;
		this.markets = markets;
		this.tipsters = tipsters;
		this.teams = teams;
	}

	private record Dashboard(Object overall, List<SegmentedBetAggregate> bySport, List<SegmentedBetAggregate> byMarket,
			List<SegmentedBetAggregate> byHouse, List<SegmentedBetAggregate> byLeague,
			List<SegmentedBetAggregate> byTipster, List<SegmentedBetAggregate> byTeam,
			List<SegmentedBetAggregate> byBetType, Object monthly) {
	}

	private Dashboard read(StatisticsFilter filter) {
		return new Dashboard(factBetRepository.aggregateOverall(filter), sorted(factBetRepository.aggregateBySport(filter)),
				sorted(factBetRepository.aggregateByMarket(filter)),
				sorted(factBetRepository.aggregateByBettingHouse(filter)),
				sorted(factBetRepository.aggregateByLeague(filter)), sorted(factBetRepository.aggregateByTipster(filter)),
				sorted(factBetRepository.aggregateByTeam(filter)), sorted(factBetRepository.aggregateByBetType(filter)),
				factBetRepository.aggregateByMonth(filter));
	}

	private static List<SegmentedBetAggregate> sorted(List<SegmentedBetAggregate> segments) {
		return segments.stream().sorted(Comparator.comparing(SegmentedBetAggregate::dimensionId)).toList();
	}

	// AVG e SUM/SUM escolhem escalas diferentes na divisao: medias iguais ate a 16a casa significativa
	private static final BigDecimal TOLERANCE = new BigDecimal("1e-12");

	private static RecursiveComparisonConfiguration numericallyEqual() {
		Comparator<BigDecimal> closeEnough = (a, b) -> a.subtract(b).abs().compareTo(TOLERANCE) <= 0 ? 0
				: a.compareTo(b);
		return RecursiveComparisonConfiguration.builder()
				.withComparatorForType(closeEnough, BigDecimal.class)
				.build();
	}

	private Integer count(String table) {
		return jdbcTemplate.queryForObject("SELECT count(*) FROM \"" + schema.value() + "\"." + table, Integer.class);
	}

	private UUID day(LocalDate date) {
		return DimDateFixtures.ensure(dimDateRepository, date.getDayOfMonth(), date.getMonthValue(), date.getYear(),
				(date.getMonthValue() - 1) / 3 + 1, date.getDayOfWeek().name());
	}

	private static FactBet bet(UUID dateId, UUID house, UUID sport, UUID league, UUID market, UUID tipster,
			UUID team1, UUID team2, String stake, String odd, String profit, BetStatus status, BetType betType) {
		return new FactBet(UUID.randomUUID(), dateId, house, sport, league, market, tipster, team1, team2,
				new BigDecimal(stake), odd == null ? null : new BigDecimal(odd),
				profit == null ? null : new BigDecimal(profit), status == BetStatus.PENDING ? null : status == BetStatus.WON,
				status, betType, 1);
	}

	@Test
	void shouldGiveTheSameNineAggregatesFromTheDailySumsAsFromFactBet() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID house1 = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa 1")).id();
			UUID house2 = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa 2")).id();
			UUID football = sports.save(new DimSport(UUID.randomUUID(), "Futebol")).id();
			UUID basketball = sports.save(new DimSport(UUID.randomUUID(), "Basquete")).id();
			UUID league1 = leagues.save(new DimLeague(UUID.randomUUID(), "Liga 1")).id();
			UUID league2 = leagues.save(new DimLeague(UUID.randomUUID(), "Liga 2")).id();
			UUID market1 = markets.save(new DimMarket(UUID.randomUUID(), "Resultado")).id();
			UUID market2 = markets.save(new DimMarket(UUID.randomUUID(), "Gols")).id();
			UUID tipster = tipsters.save(new DimTipster(UUID.randomUUID(), "Tipster")).id();
			UUID teamA = teams.save(new DimTeam(UUID.randomUUID(), "Time A", football)).id();
			UUID teamB = teams.save(new DimTeam(UUID.randomUUID(), "Time B", football)).id();
			UUID d1 = day(DAY_1);
			UUID d2 = day(DAY_2);
			UUID d3 = day(DAY_3);
			UUID outside = day(LocalDate.of(2026, 9, 30));

			factBetRepository.save(bet(d1, house1, football, league1, market1, tipster, teamA, teamB, "100.00",
					"2.10", "110.00", BetStatus.WON, BetType.PRE));
			factBetRepository.save(bet(d1, house2, football, league1, market2, null, teamA, teamA, "50.00", "1.80",
					"-50.00", BetStatus.LOST, BetType.LIVE));
			factBetRepository.save(bet(d1, house1, basketball, league2, market1, null, null, teamB, "30.00", null,
					"0.00", BetStatus.VOID, null));
			factBetRepository.save(bet(d2, house2, basketball, league2, market2, tipster, null, null, "70.00",
					"1.50", "35.00", BetStatus.WON, BetType.PRE));
			factBetRepository.save(bet(d2, house1, football, league1, market1, tipster, teamB, teamA, "20.00", null,
					"-20.00", BetStatus.LOST, null));
			factBetRepository.save(bet(d3, house1, football, league2, market2, null, teamA, null, "999.00", "3.00",
					null, BetStatus.PENDING, BetType.PRE));
			factBetRepository.save(bet(d3, house2, football, league2, market1, tipster, teamA, teamB, "40.00",
					"2.50", "60.00", BetStatus.WON, BetType.LIVE));
			factBetRepository.save(bet(outside, house1, football, league1, market1, tipster, teamA, teamB, "500.00",
					"2.00", "500.00", BetStatus.WON, BetType.PRE));

			assertThat(count("agg_dirty_day")).isEqualTo(4);
			Dashboard fromFactBet = read(PERIOD);
			Dashboard everythingFromFactBet = read(StatisticsFilter.none());

			assertThat(refreshDailyAggregates.refreshDirtyDays(31)).isEqualTo(4);

			assertThat(count("agg_dirty_day")).isZero();
			assertThat(count("agg_daily_segment")).isPositive();
			assertThat(read(PERIOD)).usingRecursiveComparison(numericallyEqual()).isEqualTo(fromFactBet);
			assertThat(read(StatisticsFilter.none())).usingRecursiveComparison(numericallyEqual())
					.isEqualTo(everythingFromFactBet);
			assertThat(fromFactBet.byTeam()).hasSize(2);
		}
	}

	@Test
	void shouldMarkTheOldAndTheNewDayWhenAFactChangesDateAndRecomputeBoth() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID house = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa")).id();
			UUID sport = sports.save(new DimSport(UUID.randomUUID(), "Futebol")).id();
			UUID league = leagues.save(new DimLeague(UUID.randomUUID(), "Liga")).id();
			UUID market = markets.save(new DimMarket(UUID.randomUUID(), "Resultado")).id();
			UUID d1 = day(DAY_1);
			UUID d2 = day(DAY_2);
			FactBet original = factBetRepository.save(bet(d1, house, sport, league, market, null, null, null,
					"100.00", "2.00", "100.00", BetStatus.WON, BetType.PRE));
			refreshDailyAggregates.refreshDirtyDays(31);

			factBetRepository.save(new FactBet(original.id(), d2, house, sport, league, market, null, null, null,
					original.stake(), original.odd(), original.profit(), true, BetStatus.WON, BetType.PRE, 1));

			assertThat(jdbcTemplate.queryForList("SELECT day FROM \"" + schema.value() + "\".agg_dirty_day ORDER BY day",
					LocalDate.class)).containsExactly(DAY_1, DAY_2);
			Dashboard fromFactBet = read(PERIOD);
			refreshDailyAggregates.refreshDirtyDays(31);
			assertThat(read(PERIOD)).usingRecursiveComparison(numericallyEqual()).isEqualTo(fromFactBet);
			assertThat(factBetRepository.aggregateOverall(new StatisticsFilter(null, null, null, null, null, DAY_1,
					DAY_1)).settledCount()).isZero();
		}
	}

	@Test
	void shouldKeepReadingFactBetWhenTheFilterHasADimension() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID house = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa")).id();
			UUID otherHouse = houses.save(new DimBettingHouse(UUID.randomUUID(), "Outra")).id();
			UUID sport = sports.save(new DimSport(UUID.randomUUID(), "Futebol")).id();
			UUID league = leagues.save(new DimLeague(UUID.randomUUID(), "Liga")).id();
			UUID market = markets.save(new DimMarket(UUID.randomUUID(), "Resultado")).id();
			UUID d1 = day(DAY_1);
			factBetRepository.save(bet(d1, house, sport, league, market, null, null, null, "100.00", "2.00",
					"100.00", BetStatus.WON, BetType.PRE));
			factBetRepository.save(bet(d1, otherHouse, sport, league, market, null, null, null, "10.00", "2.00",
					"-10.00", BetStatus.LOST, BetType.PRE));
			refreshDailyAggregates.refreshDirtyDays(31);

			StatisticsFilter byHouse = new StatisticsFilter(house, null, null, null, null, DAY_1, DAY_3);

			assertThat(factBetRepository.aggregateOverall(byHouse).totalStaked()).isEqualByComparingTo("100.00");
			assertThat(factBetRepository.aggregateOverall(PERIOD).totalStaked()).isEqualByComparingTo("110.00");
		}
	}

	@Test
	void shouldReturnTheMonthlySeriesFromFactBetInChronologicalOrder() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID house = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa")).id();
			UUID sport = sports.save(new DimSport(UUID.randomUUID(), "Futebol")).id();
			UUID league = leagues.save(new DimLeague(UUID.randomUUID(), "Liga")).id();
			UUID market = markets.save(new DimMarket(UUID.randomUUID(), "Resultado")).id();
			// meses de dois anos gravados fora de ordem: sem ORDER BY, a agregacao por hash os devolve em ordem arbitraria
			int[][] months = { { 2026, 3 }, { 2025, 11 }, { 2026, 10 }, { 2026, 1 }, { 2025, 12 }, { 2026, 7 }, { 2026, 5 },
					{ 2026, 2 }, { 2026, 9 }, { 2026, 4 }, { 2026, 8 }, { 2026, 6 } };
			for (int[] month : months) {
				factBetRepository.save(bet(day(LocalDate.of(month[0], month[1], 15)), house, sport, league, market, null,
						null, null, "10.00", "2.00", "10.00", BetStatus.WON, BetType.PRE));
			}
			StatisticsFilter bySport = new StatisticsFilter(null, sport, null, null, null, LocalDate.of(2025, 11, 1),
					LocalDate.of(2026, 10, 31));

			assertThat(factBetRepository.aggregateByMonth(bySport))
					.extracting(month -> month.year() * 100 + month.month())
					.containsExactly(202511, 202512, 202601, 202602, 202603, 202604, 202605, 202606, 202607, 202608,
							202609, 202610);
		}
	}

	@Test
	void shouldRefreshOnlyTheMostRecentDirtyDaysUpToTheLimitAndKeepTheRest() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID house = houses.save(new DimBettingHouse(UUID.randomUUID(), "Casa")).id();
			UUID sport = sports.save(new DimSport(UUID.randomUUID(), "Futebol")).id();
			UUID league = leagues.save(new DimLeague(UUID.randomUUID(), "Liga")).id();
			UUID market = markets.save(new DimMarket(UUID.randomUUID(), "Resultado")).id();
			for (LocalDate date : List.of(DAY_1, DAY_2, DAY_3)) {
				factBetRepository.save(bet(day(date), house, sport, league, market, null, null, null, "10.00",
						"2.00", "10.00", BetStatus.WON, BetType.PRE));
			}

			assertThat(refreshDailyAggregates.refreshDirtyDays(2)).isEqualTo(2);

			assertThat(jdbcTemplate.queryForList("SELECT day FROM \"" + schema.value() + "\".agg_dirty_day",
					LocalDate.class)).containsExactly(DAY_1);
			assertThat(refreshDailyAggregates.refreshDirtyDays(2)).isEqualTo(1);
			assertThat(count("agg_dirty_day")).isZero();
		}
	}

	@Test
	void shouldListTheTenantSchemaAndRefreshNothingWithoutDirtyDays() {
		assertThat(tenantSchemaGateway.listWithDailyAggregates()).contains(schema);
		try (var _ = TenantContextScope.open(schema)) {
			assertThat(refreshDailyAggregates.refreshDirtyDays(31)).isZero();
		}
	}
}
