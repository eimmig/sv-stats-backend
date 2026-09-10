package com.stakevault.betting.stats.domain.port.in;

import java.util.List;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.DimTeam;

public interface ListTeamsUseCase {

	List<DimTeam> listBySport(UUID sportId);
}
