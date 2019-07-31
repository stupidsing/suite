package suite.net;

import java.io.Closeable;

import primal.Ob;
import suite.util.Thread_;
import suite.util.Thread_.RunnableEx;

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
		thread = Thread_.startThread(serve);

		while (!started)
			Ob.wait(this);
	}

	public synchronized void stop() {
		running = false;
		thread.interrupt();

		while (started)
			Ob.wait(this);

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
