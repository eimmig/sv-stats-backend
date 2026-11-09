package com.stakevault.betting.stats.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.stakevault.betting.stats.domain.model.TenantSchemaName;

class TenantPropagatingConcurrentCallsTest {

	private final TenantPropagatingConcurrentCalls calls = new TenantPropagatingConcurrentCalls();

	@AfterEach
	void tearDown() {
		calls.destroy();
	}

	@Test
	void shouldRunTheCallOnAnotherThreadWithTheCallersTenantSchema() {
		TenantSchemaName schema = new TenantSchemaName("tenant_alpha");
		AtomicReference<Thread> callThread = new AtomicReference<>();
		Supplier<TenantSchemaName> result;
		try (var _ = TenantContextScope.open(schema)) {
			result = calls.fork(() -> {
				callThread.set(Thread.currentThread());
				return TenantContextHolder.current();
			});
		}

		assertThat(result.get()).isEqualTo(schema);
		assertThat(callThread.get()).isNotEqualTo(Thread.currentThread());
	}

	@Test
	void shouldRunWithoutATenantWhenTheCallerHasNone() {
		assertThat(calls.fork(TenantContextHolder::current).get()).isNull();
	}

	@Test
	void shouldRethrowTheCallsOwnRuntimeExceptionUnwrapped() {
		IllegalArgumentException failure = new IllegalArgumentException("bad filter");
		Supplier<Object> result = calls.fork(() -> {
			throw failure;
		});

		assertThatThrownBy(result::get).isSameAs(failure);
	}
}
