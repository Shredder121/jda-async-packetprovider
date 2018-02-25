/*
 * Copyright 2017 Shredder121.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.shredder121.asyncaudio.common;

import java.util.concurrent.*;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import lombok.experimental.UtilityClass;

@UtilityClass
public class CommonAsync {

	public static final int DEFAULT_BACKLOG = 20;

	public static ScheduledExecutorService rescheduler = Executors.newSingleThreadScheduledExecutor(
			new BasicThreadFactory.Builder()
					.daemon(true)
					.namingPattern("japp-rescheduler") // only 1 thread
			.build()
	);

	public static ThreadFactory threadFactory = new BasicThreadFactory.Builder()
			.daemon(true)
			.priority((Thread.NORM_PRIORITY + Thread.MIN_PRIORITY) / 2) // uses a lot of cpu but can back down if load is high
			.namingPattern("japp-%d")
			.wrappedFactory(CommonAsync::configuredWorkerThread)
			.build();

	public static ForkJoinPool workerPool = new ForkJoinPool(
			Runtime.getRuntime().availableProcessors(),
			__NOT_USED -> ((ForkJoinWorkerThread) threadFactory.newThread(null/*will not be used*/)),
			null,
			true // we're going to use the ForkJoinPool in queue mode
	);

	/**
	 * Creates a ForkJoinWorkerThread as if it were a ThreadFactory.
	 *
	 * <p>
	 *  {@link ForkJoinWorkerThread}s do not use Runnables as target.
	 *  They use a work queue that depends on the pool, other workers, etc.
	 * </p>
	 *
	 */
	private static ForkJoinWorkerThread configuredWorkerThread(Runnable __NOT_USED) {
		return new ForkJoinWorkerThread(workerPool) {};
	}
}
