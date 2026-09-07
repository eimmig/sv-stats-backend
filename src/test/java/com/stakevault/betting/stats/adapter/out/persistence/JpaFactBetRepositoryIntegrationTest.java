package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import com.stakevault.betting.stats.domain.model.FactBet;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimBettingHouseRepository;
import com.stakevault.betting.stats.domain.port.out.DimDateRepository;
import com.stakevault.betting.stats.domain.port.out.DimLeagueRepository;
import com.stakevault.betting.stats.domain.port.out.DimMarketRepository;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.FactBetRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaFactBetRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final FactBetRepository factBetRepository;
	private final DimDateRepository dimDateRepository;
	private final DimBettingHouseRepository dimBettingHouseRepository;
	private final DimSportRepository dimSportRepository;
	private final DimLeagueRepository dimLeagueRepository;
	private final DimMarketRepository dimMarketRepository;

	JpaFactBetRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
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

	private FactBet newFactBet() {
		UUID dateId = dimDateRepository.save(new DimDate(UUID.randomUUID(), 6, 9, 2026, 3, "SUNDAY")).id();
		UUID bettingHouseId = dimBettingHouseRepository.save(new DimBettingHouse(UUID.randomUUID(), "House")).id();
		UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Sport")).id();
		UUID leagueId = dimLeagueRepository.save(new DimLeague(UUID.randomUUID(), "League")).id();
		UUID marketId = dimMarketRepository.save(new DimMarket(UUID.randomUUID(), "Market")).id();
		return new FactBet(UUID.randomUUID(), dateId, bettingHouseId, sportId, leagueId, marketId, null,
				BigDecimal.valueOf(100), null, null, BetStatus.PENDING, 1);
	}

	@Test
	void shouldSaveAndFindById() {
		try (var _ = TenantContextScope.open(schema)) {
			FactBet factBet = newFactBet();

			FactBet saved = factBetRepository.save(factBet);
			FactBet found = factBetRepository.findById(saved.id()).orElseThrow();

			// BigDecimal.equals() distingue escala (100 vs 100.00, ver docs/TESTING.md) - compara
			// campo a campo em vez de igualdade de record inteiro.
			assertThat(found.id()).isEqualTo(saved.id());
			assertThat(found.dateId()).isEqualTo(saved.dateId());
			assertThat(found.bettingHouseId()).isEqualTo(saved.bettingHouseId());
			assertThat(found.stake()).isEqualByComparingTo(saved.stake());
			assertThat(found.status()).isEqualTo(BetStatus.PENDING);
			assertThat(found.profit()).isNull();
			assertThat(found.isWin()).isNull();
			assertThat(found.betCount()).isEqualTo(1);
		}
	}

	@Test
	void shouldUpdateExistingRowInsteadOfInsertingADuplicate() {
		try (var _ = TenantContextScope.open(schema)) {
			FactBet inserted = factBetRepository.save(newFactBet());
			FactBet settled = new FactBet(inserted.id(), inserted.dateId(), inserted.bettingHouseId(),
					inserted.sportId(), inserted.leagueId(), inserted.marketId(), inserted.tipsterId(),
					inserted.stake(), BigDecimal.valueOf(150), true, BetStatus.WON, 1);

			factBetRepository.save(settled);

			Integer rowCount = jdbcTemplate.queryForObject(
					"SELECT count(*) FROM \"" + schema.value() + "\".fact_bet WHERE id = ?", Integer.class,
					inserted.id());
			assertThat(rowCount).isEqualTo(1);
			FactBet found = factBetRepository.findById(inserted.id()).orElseThrow();
			assertThat(found.status()).isEqualTo(BetStatus.WON);
			assertThat(found.profit()).isEqualByComparingTo(BigDecimal.valueOf(150));
			assertThat(found.isWin()).isTrue();
		}
	}

	@Test
	void shouldIsolateRowsBetweenTenantSchemas() {
		String otherSlug = "test-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
		TenantSchemaName otherSchema = TenantSchemaName.fromSlug(otherSlug);
		provisionTenantSchema.ensureSchemaExists(otherSlug);

		try {
			FactBet inFirstTenant;
			FactBet inOtherTenant;
			try (var _ = TenantContextScope.open(schema)) {
				inFirstTenant = factBetRepository.save(newFactBet());
			}
			try (var _ = TenantContextScope.open(otherSchema)) {
				inOtherTenant = factBetRepository.save(newFactBet());
			}

			try (var _ = TenantContextScope.open(schema)) {
				assertThat(factBetRepository.findById(inFirstTenant.id())).isPresent();
				assertThat(factBetRepository.findById(inOtherTenant.id())).isEmpty();
			}
			try (var _ = TenantContextScope.open(otherSchema)) {
				assertThat(factBetRepository.findById(inOtherTenant.id())).isPresent();
				assertThat(factBetRepository.findById(inFirstTenant.id())).isEmpty();
			}
		} finally {
			jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + otherSchema.value() + "\" CASCADE");
		}
	}
}
