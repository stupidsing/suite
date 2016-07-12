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

	public synchronized <T> T thenNotify(Source<T> source) {
		T t = source.source();
		if (cond.ok())
			notify();
		return t;
	}

	public synchronized <T> T thenNotifyAll(Source<T> source) {
		T t = source.source();
		if (cond.ok())
			notifyAll();
		return t;
	}

	public <T> T waitThen(Source<T> source) {
		return waitThen(0, source);
	}

	public synchronized <T> T waitThen(int timeOut, Source<T> source) {
		while (!cond.ok()) {
			try {
				wait(timeOut);
			} catch (InterruptedException e) {
			}
		}
		return source.source();
	}

}
