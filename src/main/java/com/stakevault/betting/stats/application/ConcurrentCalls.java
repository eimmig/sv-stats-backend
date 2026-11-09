package com.stakevault.betting.stats.application;

import java.util.function.Supplier;

/**
 * Starts a call on another thread and returns a handle that waits for its result. Implementations
 * carry the caller's tenant context to the other thread, so a repository call inside the task reads
 * the same schema as the caller.
 */
public interface ConcurrentCalls {

	/**
	 * Starts {@code call} and returns a supplier that blocks until the call finishes. The supplier
	 * rethrows the call's own {@link RuntimeException} or {@link Error}, unwrapped.
	 */
	<T> Supplier<T> fork(Supplier<T> call);
}
