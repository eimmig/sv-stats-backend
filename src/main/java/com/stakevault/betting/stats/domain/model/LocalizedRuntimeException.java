package com.stakevault.betting.stats.domain.model;

public abstract class LocalizedRuntimeException extends RuntimeException implements LocalizedDomainException {

	private final transient Object[] args;

	protected LocalizedRuntimeException(String message, Object... args) {
		super(message);
		this.args = args;
	}

	protected LocalizedRuntimeException(String message, Throwable cause, Object... args) {
		super(message, cause);
		this.args = args;
	}

	@Override
	public Object[] messageArgs() {
		return args.clone();
	}
}
