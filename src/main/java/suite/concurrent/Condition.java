package suite.concurrent;

import static suite.util.Fail.fail;

import suite.streamlet.FunUtil.Source;

public class Condition {

	public interface Cond {
		public boolean ok();
	}

	public synchronized void satisfyOne(Runnable sat) {
		sat.run();
		notify();
	}

	public synchronized void satisfyAll(Runnable sat) {
		sat.run();
		notifyAll();
	}

	public void waitTill(Cond cond) {
		waitTill(cond, this::getClass, this::getClass, Long.MAX_VALUE);
	}

	public synchronized <T> T waitTill(Cond cond, Runnable before, Source<T> after, long timeout) {
		var now = System.currentTimeMillis();
		var start = now;

		while (!cond.ok()) {
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

		return after.g();
	}

}
