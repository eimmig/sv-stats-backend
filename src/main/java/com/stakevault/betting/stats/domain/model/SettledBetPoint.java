package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SettledBetPoint(LocalDate date, BigDecimal profit) {
}
