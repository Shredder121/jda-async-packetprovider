package com.github.shredder121.asyncaudio.common;

import lombok.experimental.NonFinal;

import java.net.DatagramPacket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class ProvideForkJoinTask extends ForkJoinTask<ByteBuffer> {

	Supplier<ByteBuffer> provider;
	BlockingQueue<ByteBuffer> queue;

	/**
	 * A reference to a {@link DatagramPacket} that couldn't be offered.
	 *
	 * <p>
	 *   Set to a non-null value when a round needs to be skipped.
	 *   The packet will then be picked up on a subsequent run.
	 * </p>
	 */
	AtomicReference<ByteBuffer> packetRef = new AtomicReference<>();

	@NonFinal //flag to stop
	private volatile boolean stopRequested; //false

	public ProvideForkJoinTask(Supplier<ByteBuffer> provider, BlockingQueue<ByteBuffer> queue) {
		this.provider = provider;
		this.queue = queue;
	}

	@Override
	protected boolean exec() {
		ByteBuffer packet = this.packetRef.getAndSet(null);
		if (this.stopRequested) {
			return true;
		} else if (packet == null) {
			packet = optionallyCopyData(this.provider.get());
		}

		if (packet == null || !this.queue.offer(packet)) {
			// offer failed, retry next run
			this.packetRef.set(packet);
			ForkJoinPool pool = getPool();
			ForkJoinTask<?> me = this;

			// instead of spinning, suspend execution
			reinitialize();
			CommonAsync.rescheduler.schedule(() -> pool.execute(me), 40, TimeUnit.MILLISECONDS);
			return false;
		}

		reinitialize();
		fork();
		return false;
	}

	/**
	 * Makes a copy of the data in the packet to make the packet safe to store for longer periods of time.
	 *
	 * <p>
	 *   JDA changed to have only one backing array for all packets, to reduce allocations.
	 *   This means that in JAPP code we have to copy the packet data before storing it for longer periods of time.
	 * </p>
	 *
	 * @param packet the packet to copy the data for
	 * @return the adjusted packet
	 */
	private static ByteBuffer optionallyCopyData(ByteBuffer packet) {
		if (packet != null) {
			ByteBuffer buffer = ByteBuffer.allocate(packet.capacity());
			buffer.put(packet);
			((Buffer) buffer).flip();
			return buffer;
		}
		return null;
	}

	@Override
	protected void setRawResult(ByteBuffer value) {
		throw new UnsupportedOperationException("Not needed.");
	}

	@Override
	public ByteBuffer getRawResult() {
		throw new UnsupportedOperationException("Not needed.");
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		this.stopRequested = true;
		return super.cancel(mayInterruptIfRunning);
	}
}
