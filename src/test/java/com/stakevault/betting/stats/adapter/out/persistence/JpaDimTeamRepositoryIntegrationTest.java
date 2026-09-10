package com.stakevault.betting.stats.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.stakevault.betting.stats.config.TenantContextScope;
import com.stakevault.betting.stats.domain.model.DimSport;
import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.in.ProvisionTenantSchemaUseCase;
import com.stakevault.betting.stats.domain.port.out.DimSportRepository;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;
import com.stakevault.betting.stats.support.TenantSchemaIntegrationSupport;

class JpaDimTeamRepositoryIntegrationTest extends TenantSchemaIntegrationSupport {

	private final DimTeamRepository dimTeamRepository;
	private final DimSportRepository dimSportRepository;

	JpaDimTeamRepositoryIntegrationTest(ProvisionTenantSchemaUseCase provisionTenantSchema, JdbcTemplate jdbcTemplate,
			DimTeamRepository dimTeamRepository, DimSportRepository dimSportRepository) {
		super(provisionTenantSchema, jdbcTemplate);
		this.dimTeamRepository = dimTeamRepository;
		this.dimSportRepository = dimSportRepository;
	}

	@Test
	void shouldSaveAndFindByNaturalKey() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			assertThat(dimTeamRepository.findByNameAndSportId("Flamengo", sportId)).isEmpty();

			UUID id = UUID.randomUUID();
			dimTeamRepository.save(new DimTeam(id, "Flamengo", sportId));

			DimTeam found = dimTeamRepository.findByNameAndSportId("Flamengo", sportId).orElseThrow();
			assertThat(found.id()).isEqualTo(id);
			assertThat(found.sportId()).isEqualTo(sportId);
		}
	}

	// feat-013: o mesmo nome de time pode existir em esportes diferentes - name sozinho nao e
	// chave natural suficiente, sportId completa a chave composta.
	@Test
	void shouldTreatSameNameInDifferentSportsAsDistinctRows() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID soccerId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			UUID basketballId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Basketball")).id();

			DimTeam soccerFlamengo = dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Flamengo", soccerId));
			DimTeam basketballFlamengo = dimTeamRepository
					.save(new DimTeam(UUID.randomUUID(), "Flamengo", basketballId));

			assertThat(soccerFlamengo.id()).isNotEqualTo(basketballFlamengo.id());
			assertThat(dimTeamRepository.findBySportId(soccerId)).containsExactly(soccerFlamengo);
			assertThat(dimTeamRepository.findBySportId(basketballId)).containsExactly(basketballFlamengo);
		}
	}

	// feat-013: prova a UNIQUE(name, sport_id) no banco, nao so o caminho de aplicacao (que ja
	// evita duplicata via findByNameAndSportId antes do save) - mesmo padrao de
	// JpaProcessedEventRepositoryIntegrationTest.shouldRejectDuplicateEventIdAtTheDatabaseLevel.
	@Test
	void shouldRejectDuplicateNameAndSportAtTheDatabaseLevel() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Flamengo", sportId));
			DimTeam duplicate = new DimTeam(UUID.randomUUID(), "Flamengo", sportId);

			assertThatThrownBy(() -> dimTeamRepository.save(duplicate))
					.isInstanceOf(DataIntegrityViolationException.class);
		}
	}

	@Test
	void shouldListTeamsOrderedByNameForASport() {
		try (var _ = TenantContextScope.open(schema)) {
			UUID sportId = dimSportRepository.save(new DimSport(UUID.randomUUID(), "Soccer")).id();
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Vasco", sportId));
			dimTeamRepository.save(new DimTeam(UUID.randomUUID(), "Flamengo", sportId));

			List<DimTeam> teams = dimTeamRepository.findBySportId(sportId);

			assertThat(teams).extracting(DimTeam::name).containsExactly("Flamengo", "Vasco");
		}
	}
}
