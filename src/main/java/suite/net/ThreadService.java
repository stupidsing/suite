package suite.net;

import primal.Verbs.RunnableEx;
import primal.Verbs.Start;
import primal.Verbs.Wait;

import java.io.Closeable;

public class ThreadService {

	private boolean started;
	private RunnableEx serve;
	private Thread thread;
	private volatile boolean running = false;

	public ThreadService(RunnableEx serve) {
		this.serve = serve;
	}

	public synchronized void start() {
		running = true;
		thread = Start.thread(serve);

		while (!started)
			Wait.object(this);
	}

	public synchronized void stop() {
		running = false;
		thread.interrupt();

		while (started)
			Wait.object(this);

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
