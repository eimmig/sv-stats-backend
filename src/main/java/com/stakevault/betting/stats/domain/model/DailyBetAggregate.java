package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyBetAggregate(LocalDate date, BigDecimal totalStaked, BigDecimal netProfit, long betCount) {
}
