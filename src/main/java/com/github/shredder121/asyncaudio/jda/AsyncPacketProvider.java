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

import com.github.shredder121.asyncaudio.common.CommonAsync;
import com.github.shredder121.asyncaudio.common.ProvideForkJoinTask;
import lombok.experimental.Delegate;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.audio.factory.IPacketProvider;

import javax.annotation.Nullable;
import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An adapter class that wraps an audio send handler to provide frame data in an async fashion.
 *
 * @author Shredder121
 */
@Slf4j
class AsyncPacketProvider implements IPacketProvider {

	static AsyncPacketProvider wrap(IPacketProvider provider, int backlog, AtomicReference<Future<?>> taskRef) {
		return new AsyncPacketProvider(provider, backlog, taskRef);
	}

	@Delegate(excludes = ActualProvide.class)
	IPacketProvider packetProvider;

	BlockingQueue<ByteBuffer> queue;

	AtomicBoolean talking = new AtomicBoolean();

	private AsyncPacketProvider(IPacketProvider packetProvider, int backlog, AtomicReference<Future<?>> taskRef) {
		this.packetProvider = packetProvider;
		this.queue = new ArrayBlockingQueue<>(backlog);

		taskRef.updateAndGet(__ -> CommonAsync.workerPool.submit(new ProvideForkJoinTask(
				() -> this.packetProvider.getNextPacketRaw(this.talking.get()),
				this.queue
		)));
	}

	@Override
	public ByteBuffer getNextPacketRaw(boolean changeTalking) {
		this.talking.set(changeTalking);
		return this.queue.poll();
	}

	@Nullable
	@Override
	public DatagramPacket getNextPacket(boolean changeTalking) {
		ByteBuffer buffer = getNextPacketRaw(changeTalking);
		if (buffer == null)
			return null;
		byte[] data = buffer.array();
		int offset = buffer.arrayOffset() + buffer.position();
		int length = buffer.remaining();
		return new DatagramPacket(data, offset, length, getSocketAddress());
	}

	private interface ActualProvide {
		ByteBuffer getNextPacketRaw(boolean changeTalking);
		DatagramPacket getNextPacket(boolean changeTalking);
	}
}
