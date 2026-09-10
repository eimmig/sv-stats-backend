package com.stakevault.betting.stats.domain.model;

// GET /api/v1/statistics/search (epic-011) exige sportId/leagueId - unico ponto do contrato onde
// um filtro de estatistica deixa de ser opcional (RN08 continua regendo o dashboard consolidado,
// GET /api/v1/statistics). Mesmo padrao de LocalizedDomainException/DomainExceptionHandler ja
// usado no servico, para o erro sair no formato RFC 7807 localizado em vez do 400 generico do
// Spring MVC (achado real do plan review de epic-011).
public class MissingRequiredStatisticsFilterException extends RuntimeException implements LocalizedDomainException {

	private final String fieldName;

	public MissingRequiredStatisticsFilterException(String fieldName) {
		super("missing required statistics search filter: " + fieldName);
		this.fieldName = fieldName;
	}

	public String fieldName() {
		return fieldName;
	}

	@Override
	public String messageKey() {
		return "error.statistics-search-missing-filter";
	}

	@Override
	public int httpStatusCode() {
		return 400;
	}

	@Override
	public Object[] messageArgs() {
		return new Object[] { fieldName };
	}
}
