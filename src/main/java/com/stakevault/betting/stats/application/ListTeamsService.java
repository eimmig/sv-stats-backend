package com.stakevault.betting.stats.application;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.stakevault.betting.stats.domain.model.DimTeam;
import com.stakevault.betting.stats.domain.port.in.ListTeamsUseCase;
import com.stakevault.betting.stats.domain.port.out.DimTeamRepository;

@Service
public class ListTeamsService implements ListTeamsUseCase {

	private final DimTeamRepository teamRepository;

	public ListTeamsService(DimTeamRepository teamRepository) {
		this.teamRepository = teamRepository;
	}

	@Override
	public List<DimTeam> listBySport(UUID sportId) {
		return teamRepository.findBySportId(sportId);
	}
}
