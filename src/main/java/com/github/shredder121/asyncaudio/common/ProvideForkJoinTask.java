package com.github.shredder121.asyncaudio.common;

import java.net.DatagramPacket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import lombok.experimental.NonFinal;

public class ProvideForkJoinTask extends ForkJoinTask<DatagramPacket> {

	Supplier<DatagramPacket> provider;
	BlockingQueue<DatagramPacket> queue;

	/**
	 * A reference to a {@link DatagramPacket} that couldn't be offered.
	 *
	 * <p>
	 *   Set to a non-null value when a round needs to be skipped.
	 *   The packet will then be picked up on a subsequent run.
	 * </p>
	 */
	AtomicReference<DatagramPacket> packetRef = new AtomicReference<>();


	@NonFinal //flag to stop
	private volatile boolean stopRequested; //false

	public ProvideForkJoinTask(Supplier<DatagramPacket> provider, BlockingQueue<DatagramPacket> queue) {
		this.provider = provider;
		this.queue = queue;
	}

    @Override
    protected boolean exec() {
		DatagramPacket packet = this.packetRef.getAndSet(null);
		if (this.stopRequested) {
			return true;
		} else if (packet == null) {
			packet = this.provider.get();
		}

        if (packet == null || !this.queue.offer(packet)) {
		    // offer failed, retry in next run
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

    @Override
    protected void setRawResult(DatagramPacket value) {
		throw new UnsupportedOperationException("Not needed.");
    }

	@Override
	public DatagramPacket getRawResult() {
		throw new UnsupportedOperationException("Not needed.");
	}

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        this.stopRequested = true;
        return super.cancel(mayInterruptIfRunning);
    }
}

