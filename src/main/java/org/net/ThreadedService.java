package org.net;

import java.io.Closeable;

import org.util.LogUtil;
import org.util.Util;

public abstract class ThreadedService {

	private boolean started;
	private Thread thread;
	protected volatile boolean running = false;

	protected abstract void serve() throws Exception;

	public synchronized void start() {
		running = true;
		thread = new Thread() {
			public void run() {
				try {
					serve();
				} catch (Exception ex) {
					LogUtil.error(ex);
				}
			}
		};

		thread.start();

		while (started != true)
			Util.wait(this);
	}

	public synchronized void stop() {
		running = false;
		thread.interrupt();

		while (started != false)
			Util.wait(this);

		thread = null;
	}

	public boolean isStarted() {
		return started;
	}

	public boolean isRunning() {
		return running;
	}

	protected Closeable started() {
		setStarted(true);
		return new Closeable() {
			public void close() {
				setStarted(false);
			}
		};
	}

	private synchronized void setStarted(boolean isStarted) {
		started = isStarted;
		notify();
	}

}
