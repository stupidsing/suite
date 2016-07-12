package suite.concurrent;

import suite.util.FunUtil.Source;

public class Condition {

	public interface Cond {
		public boolean ok();
	}

	private Cond cond;

	public Condition(Cond cond) {
		this.cond = cond;
	}

	public synchronized void thenNotify(Runnable runnable) {
		runnable.run();
		if (verify())
			notify();
	}

	public synchronized void thenNotifyAll(Runnable runnable) {
		runnable.run();
		if (verify())
			notifyAll();
	}

	public <T> T waitThen(Runnable before, Source<T> after) {
		return waitThen(before, 0, after);
	}

	public synchronized <T> T waitThen(Runnable before, int timeOut, Source<T> source) {
		while (!verify()) {
			before.run();
			try {
				wait(timeOut);
			} catch (InterruptedException e) {
			}
		}
		return source.source();
	}

	public boolean verify() {
		return cond.ok();
	}

}
