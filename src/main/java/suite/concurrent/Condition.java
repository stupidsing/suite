package suite.concurrent;

import static suite.util.Friends.fail;

import suite.streamlet.FunUtil.Source;

public class Condition {

	public interface Cond {
		public boolean ok();
	}

	private Cond cond;

	public Condition(Cond cond) {
		this.cond = cond;
	}

	public synchronized void satisfyOne(Runnable sat) {
		sat.run();
		if (verify())
			notify();
	}

	public synchronized void satisfyAll(Runnable sat) {
		sat.run();
		if (verify())
			notifyAll();
	}

	public <T> T waitThen(Runnable before, Source<T> after) {
		return waitThen(before, after, Long.MAX_VALUE);
	}

	public synchronized <T> T waitThen(Runnable before, Source<T> after, long timeout) {
		var now = System.currentTimeMillis();
		var start = now;

		while (!verify()) {
			before.run();

			var duration = start + timeout - now;
			if (0l < duration)
				try {
					wait(duration);
					now = System.currentTimeMillis();
				} catch (Exception ex) {
					return fail(ex);
				}
			else
				return null;
		}

		return after.source();
	}

	public boolean verify() {
		return cond.ok();
	}

}
