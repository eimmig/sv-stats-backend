package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.BetType;
import com.stakevault.betting.stats.domain.model.FactBet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "fact_bet")
@Getter
@NoArgsConstructor
public class FactBetJpaEntity extends AbstractJpaEntity {

	@Column(name = "date_id", nullable = false)
	private UUID dateId;

	@Column(name = "betting_house_id", nullable = false)
	private UUID bettingHouseId;

	@Column(name = "sport_id", nullable = false)
	private UUID sportId;

	@Column(name = "league_id", nullable = false)
	private UUID leagueId;

	@Column(name = "market_id", nullable = false)
	private UUID marketId;

	@Column(name = "tipster_id")
	private UUID tipsterId;

	@Column(name = "team1_id")
	private UUID team1Id;

	@Column(name = "team2_id")
	private UUID team2Id;

	@Column(nullable = false)
	private BigDecimal stake;

	private BigDecimal odd;

	private BigDecimal profit;

	@Column(name = "is_win")
	private Boolean isWin;

	@Column(nullable = false)
	private BetStatus status;

	@Column(name = "bet_type")
	private BetType betType;

	@Column(name = "bet_count", nullable = false)
	private int betCount;

	public FactBetJpaEntity(FactBet factBet) {
		super(factBet.id());
		applyFrom(factBet);
	}

	// Upsert real (BetSettled sobre uma linha ja inserida por BetCreated, ou o inverso) exige
	// mutar a instancia JA RASTREADA pelo Hibernate (isNew=false via @PostLoad), nao construir
	// uma nova a cada save (AbstractJpaEntity sempre marca isNew=true numa instancia recem-
	// construida, o que faria Hibernate tentar INSERT de novo e falhar por chave duplicada) -
	// metodo da propria entidade em vez de @Setter amplo, ver docs/CONVENTIONS.md.
	void applyFrom(FactBet factBet) {
		this.dateId = factBet.dateId();
		this.bettingHouseId = factBet.bettingHouseId();
		this.sportId = factBet.sportId();
		this.leagueId = factBet.leagueId();
		this.marketId = factBet.marketId();
		this.tipsterId = factBet.tipsterId();
		this.team1Id = factBet.team1Id();
		this.team2Id = factBet.team2Id();
		this.stake = factBet.stake();
		this.odd = factBet.odd();
		this.profit = factBet.profit();
		this.isWin = factBet.isWin();
		this.status = factBet.status();
		this.betType = factBet.betType();
		this.betCount = factBet.betCount();
	}
}
