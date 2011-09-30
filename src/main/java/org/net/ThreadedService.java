package org.net;

import org.util.LogUtil;
import org.util.Util;

public abstract class ThreadedService {

	private boolean started;
	private Thread thread;
	protected volatile boolean running = false;

	protected abstract void serve() throws Exception;

	public synchronized void spawn() {
		running = true;

		thread = new Thread() {
			public void run() {
				try {
					serve();
				} catch (Exception ex) {
					LogUtil.error(getClass(), ex);
				}
			}
		};

		thread.start();

		while (started != true)
			Util.wait(this);
	}

	public synchronized void unspawn() {
		running = false;

		thread.interrupt();

		while (started != false)
			Util.wait(this);
	}

	protected synchronized void setStarted(boolean isStarted) {
		started = isStarted;
		notify();
	}

}
