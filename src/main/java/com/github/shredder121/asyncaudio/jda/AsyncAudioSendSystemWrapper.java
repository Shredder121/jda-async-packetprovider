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
package com.github.shredder121.asyncaudio.jda;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;

import net.dv8tion.jda.core.audio.factory.IAudioSendSystem;

/**
 * A lifecycle manager for the taskRef.
 *
 * @author Shredder121
 */
@lombok.RequiredArgsConstructor(staticName = "wrap")
class AsyncAudioSendSystemWrapper implements IAudioSendSystem {

	IAudioSendSystem wrapped;

	AtomicReference<Future<?>> taskRef;

	@Override
	public void start() {
		this.wrapped.start();
	}

	@Override
	public void shutdown() {
		this.taskRef.updateAndGet(value -> {
			value.cancel(true);
			return null;
		});
		this.wrapped.shutdown();
	}
}
