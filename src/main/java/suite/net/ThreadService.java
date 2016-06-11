package suite.net;

import java.io.Closeable;

import suite.util.Util;
import suite.util.Util.RunnableEx;

public class ThreadService implements Service {

	private boolean started;
	private RunnableEx serve;
	private Thread thread;
	private volatile boolean running = false;

	public ThreadService(RunnableEx serve) {
		this.serve = serve;
	}

	public synchronized void start() {
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
