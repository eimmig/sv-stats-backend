package com.stakevault.betting.stats.domain.model;

public class MissingRequiredStatisticsFilterException extends LocalizedRuntimeException {

	private final String fieldName;

	public MissingRequiredStatisticsFilterException(String fieldName) {
		super("missing required statistics search filter: " + fieldName, fieldName);
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
}
