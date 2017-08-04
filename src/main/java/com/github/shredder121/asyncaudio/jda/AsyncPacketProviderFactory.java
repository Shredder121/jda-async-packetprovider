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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import com.github.shredder121.asyncaudio.jda.AsyncPacketProvider.Buddy;

import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.core.audio.factory.IAudioSendFactory;
import net.dv8tion.jda.core.audio.factory.IAudioSendSystem;
import net.dv8tion.jda.core.audio.factory.IPacketProvider;

/**
 * An audio send factory that wraps an existing audio send factory, but keeps a backlog of packets ready.
 * <p>
 *
 * The backlog is customizable, default backlog is {@value #DEFAULT_BACKLOG}.
 * <p>
 *
 * Keep in mind that having a bigger backlog means that when you do seeking, skipping, etc.
 * the previous packets in the backlog still need to be run through.
 *
 * @author Shredder121
 */
@RequiredArgsConstructor(staticName = "adapt")
public class AsyncPacketProviderFactory implements IAudioSendFactory {

	private static final int DEFAULT_BACKLOG = 20;

	static final Executor executor = Executors.newCachedThreadPool();

	/**
	 * Wrap the given factory with the default backlog. ({@value #DEFAULT_BACKLOG})
	 *
	 * @param factory the factory to be wrapped
	 * @return
	 */
	public static AsyncPacketProviderFactory adapt(IAudioSendFactory factory) {
		return AsyncPacketProviderFactory.adapt(factory, DEFAULT_BACKLOG);
	}

	/**
	 * The wrapped audio send factory.
	 */
	IAudioSendFactory factory;

	/**
	 * How many packets to keep a backlog of per PacketProvider.
	 */
	int backlog;

	@Override
	public IAudioSendSystem createSendSystem(IPacketProvider packetProvider) {
		// to be able to introduce the buddy to both parties
		AtomicReference<Buddy> buddy = new AtomicReference<>();

		AsyncPacketProvider provider = AsyncPacketProvider.wrap(packetProvider, backlog, buddy);
		AsyncAudioSendSystemWrapper system = AsyncAudioSendSystemWrapper.wrap(
				this.factory.createSendSystem(provider),
				buddy
		);
		return system;
	}
}
