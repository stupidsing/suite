package suite.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

import primal.fp.Funs.Source;

public class Fut<T> {

	private Source<T> source;
	private T t;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private Condition condition = new Condition();

	public static <T> Fut<T> of(Source<T> source) {
		var fut = new Fut<T>();
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
