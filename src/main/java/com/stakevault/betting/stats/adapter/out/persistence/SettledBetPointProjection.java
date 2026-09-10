package com.stakevault.betting.stats.adapter.out.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;

interface SettledBetPointProjection {

	LocalDate getDate();

	BigDecimal getProfit();
}
