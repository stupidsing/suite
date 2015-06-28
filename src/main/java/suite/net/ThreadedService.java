package suite.net;

import java.io.Closeable;

import suite.util.Util;

public abstract class ThreadedService {

	private boolean started;
	private Thread thread;
	protected volatile boolean running = false;

	protected abstract void serve() throws Exception;

	public synchronized void start() {
		running = true;
		thread = Util.startThread(this::serve);

		while (!started)
			Util.wait(this);
	}

	public synchronized void stop() {
		running = false;
		thread.interrupt();

		while (started)
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
		return () -> setStarted(false);
	}

	private synchronized void setStarted(boolean isStarted) {
		started = isStarted;
		notify();
	}

}
