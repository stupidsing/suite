package suite.concurrent;

import primal.fp.Funs.Source;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Run a calculation only once but allow many threads to wait for the result.
 * 
 * @author ywsing
 */
public class RunOnce<T> {

	private Source<T> source;
	private T t;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private Condition condition = new Condition();

	public static <T> RunOnce<T> of(Source<T> source) {
		var fut = new RunOnce<T>();
		fut.source = source;
		return fut;
	}

	public T get() {
		if (isRunning.getAndSet(true))
			condition.waitTill(() -> t != null);
		else
			condition.satisfyAll(() -> t = source.g());
		return t;
	}

}
