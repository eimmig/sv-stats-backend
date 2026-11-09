package com.stakevault.betting.stats.config;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

import com.stakevault.betting.stats.application.ConcurrentCalls;
import com.stakevault.betting.stats.domain.model.TenantSchemaName;

/**
 * Runs each call on a virtual thread with the caller's tenant schema. The schema lives in a
 * ThreadLocal (TenantContextHolder), which a new thread does not inherit, so it is captured on the
 * caller's thread and set again inside the task.
 */
@Component
public class TenantPropagatingConcurrentCalls implements ConcurrentCalls, DisposableBean {

	private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

	@Override
	public <T> Supplier<T> fork(Supplier<T> call) {
		TenantSchemaName schema = TenantContextHolder.current();
		Future<T> future = executor.submit(() -> {
			if (schema == null) {
				return call.get();
			}
			try (var _ = TenantContextScope.open(schema)) {
				return call.get();
			}
		});
		return () -> await(future);
	}

	private static <T> T await(Future<T> future) {
		try {
			return future.get();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			future.cancel(true);
			throw new CancellationException("interrupted while waiting for a concurrent call");
		} catch (ExecutionException e) {
			Throwable cause = e.getCause();
			if (cause instanceof RuntimeException runtime) {
				throw runtime;
			}
			if (cause instanceof Error error) {
				throw error;
			}
			throw new IllegalStateException(cause);
		}
	}

	@Override
	public void destroy() {
		executor.close();
	}
}
