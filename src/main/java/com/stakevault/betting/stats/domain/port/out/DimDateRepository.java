package com.stakevault.betting.stats.domain.port.out;

import java.util.Optional;

import com.stakevault.betting.stats.domain.model.DimDate;

public interface DimDateRepository {

	DimDate save(DimDate dimDate);

	// Diferente das demais dimensoes (id = mesmo uuid do catalogo em bets-service), uma data nao
	// chega com id proprio no evento - a linha e localizada pela chave natural (dia/mes/ano) e
	// criada sob demanda na primeira aposta daquele dia (feat-003).
	Optional<DimDate> findByDayAndMonthAndYear(int day, int month, int year);
}
