package com.stakevault.betting.stats.domain.port.out;

import java.util.Optional;
import java.util.UUID;

import com.stakevault.betting.stats.domain.model.FactBet;

public interface FactBetRepository {

	FactBet save(FactBet factBet);

	Optional<FactBet> findById(UUID id);
}
