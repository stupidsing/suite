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

	public synchronized void thenNotify(Runnable before) {
		before.run();
		if (verify())
			notify();
	}

	public synchronized void thenNotifyAll(Runnable before) {
		before.run();
		if (verify())
			notifyAll();
	}

	public synchronized <T> T waitThen(Runnable before, Source<T> after) {
		while (!verify()) {
			before.run();
			try {
				wait(0);
			} catch (InterruptedException e) {
			}
		}
		return after.source();
	}

	public boolean verify() {
		return cond.ok();
	}

}
