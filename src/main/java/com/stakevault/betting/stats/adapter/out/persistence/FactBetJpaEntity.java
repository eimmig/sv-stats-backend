package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.BetStatus;
import com.stakevault.betting.stats.domain.model.FactBet;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Unica entidade deste servico com setters - upsert real (BetSettled sobre uma linha ja
// inserida por BetCreated, ou o inverso) exige mutar a instancia JA RASTREADA pelo Hibernate
// (isNew=false via @PostLoad), nao construir uma nova a cada save (AbstractJpaEntity sempre
// marca isNew=true numa instancia recem-construida, o que faria Hibernate tentar INSERT de
// novo e falhar por chave duplicada) - ver JpaFactBetRepository e docs/CONVENTIONS.md.
@Entity
@Table(name = "fact_bet")
@Getter
@Setter
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

	@Column(nullable = false)
	private BigDecimal stake;

	private BigDecimal profit;

	@Column(name = "is_win")
	private Boolean isWin;

	@Column(nullable = false)
	private BetStatus status;

	@Column(name = "bet_count", nullable = false)
	private int betCount;

	public FactBetJpaEntity(FactBet factBet) {
		super(factBet.id());
		this.dateId = factBet.dateId();
		this.bettingHouseId = factBet.bettingHouseId();
		this.sportId = factBet.sportId();
		this.leagueId = factBet.leagueId();
		this.marketId = factBet.marketId();
		this.tipsterId = factBet.tipsterId();
		this.stake = factBet.stake();
		this.profit = factBet.profit();
		this.isWin = factBet.isWin();
		this.status = factBet.status();
		this.betCount = factBet.betCount();
	}
}
