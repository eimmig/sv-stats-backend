package com.stakevault.betting.stats.domain.model;

import java.math.BigDecimal;

public record ConfidenceInterval(BigDecimal lower, BigDecimal upper) {
}
