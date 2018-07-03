package suite.concurrent;

import suite.util.FunUtil.Source;
import suite.util.Rethrow;

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
			Rethrow.ex(() -> {
				wait(0);
				return this;
			});
		}
		return after.source();
	}

	public boolean verify() {
		return cond.ok();
	}

}
