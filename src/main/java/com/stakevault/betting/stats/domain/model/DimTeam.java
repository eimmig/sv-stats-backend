package com.stakevault.betting.stats.domain.model;

import java.util.UUID;

public record DimTeam(UUID id, String name, UUID sportId) {
}
