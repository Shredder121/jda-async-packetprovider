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
package com.github.shredder121.asyncaudio.jdaaudio;

import static java.util.concurrent.TimeUnit.SECONDS;

import java.net.DatagramPacket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import lombok.experimental.NonFinal;
import lombok.experimental.PackagePrivate;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.audio.factory.IPacketProvider;

/**
 * An adapter class that wraps an audio send handler to provide frame data in an async fashion.
 *
 * @author Shredder121
 */
@Slf4j
class AsyncPacketProvider implements IPacketProvider {

	static AsyncPacketProvider wrap(IPacketProvider provider, int backlog, AtomicReference<Buddy> buddy) {
		return new AsyncPacketProvider(provider, backlog, buddy);
	}

	@PackagePrivate
	@Delegate(excludes = ActualProvide.class)
	IPacketProvider packetProvider;

	@PackagePrivate
	int backlog;

	@PackagePrivate
	Buddy buddy;

	private AsyncPacketProvider(IPacketProvider packetProvider, int backlog, AtomicReference<Buddy> buddy) {
		this.packetProvider = packetProvider;
		this.backlog = backlog;
		this.buddy = buddy.updateAndGet(__ -> this.new Buddy());
		AsyncPacketProviderFactory.executor.execute(this.buddy);
	}

	@Override
	public DatagramPacket getNextPacket(boolean changeTalking) {
		this.buddy.changeTalking = changeTalking;
		return this.buddy.getPacket();
	}

	private interface ActualProvide {

		DatagramPacket getNextPacket(boolean changeTalking);
	}

	@RequiredArgsConstructor
	class Buddy implements Runnable {

		@NonFinal
		@PackagePrivate
		volatile boolean stopRequested;

		@NonFinal
		@PackagePrivate
		volatile boolean changeTalking;

		BlockingQueue<DatagramPacket> queue = new ArrayBlockingQueue<>(
				// keep a queue of this many packets ready
				AsyncPacketProvider.this.backlog
		);

		DatagramPacket getPacket() {
			return this.queue.poll();
		}

		@Override
		public void run() {
			try {
				do {
					DatagramPacket packet = AsyncPacketProvider.this.packetProvider.getNextPacket(this.changeTalking);
					if (packet == null) {
						 //actual value doesn't matter, as long as the thread gets taken out of scheduling
						Thread.sleep(40);
					} else if(!this.queue.offer(packet, 1, SECONDS) && !this.stopRequested) {
						AsyncPacketProvider.log.warn("clock leap or something? Trying again.");
						if (!this.queue.offer(packet, 5, SECONDS) && !this.stopRequested) {
							AsyncPacketProvider.log.warn("Missed a packet, queue is not being drained. Audio send system shutdown?");
						}
					}
				} while (!this.stopRequested);
			} catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
			}
		}
	}
}
