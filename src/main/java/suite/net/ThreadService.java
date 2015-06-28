package suite.net;

import java.io.Closeable;

import suite.util.Util;
import suite.util.Util.RunnableEx;

public class ThreadService {

	private boolean started;
	private Thread thread;
	private volatile boolean running = false;

	public synchronized void start(RunnableEx serve) {
		running = true;
		thread = Util.startThread(serve);

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

	public Closeable started() {
		setStarted(true);
		return () -> setStarted(false);
	}

	private synchronized void setStarted(boolean isStarted) {
		started = isStarted;
		notify();
	}

}
