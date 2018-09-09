package suite.concurrent;

import java.util.concurrent.atomic.AtomicBoolean;

import suite.streamlet.FunUtil.Source;

public class Fut<T> {

	private Source<T> source;
	private T t;
	private AtomicBoolean isRunning = new AtomicBoolean(false);
	private Condition condition = new Condition();

	public Fut(Source<T> source) {
		this.source = source;
	}

	public T get() {
		if (isRunning.getAndSet(true))
			condition.waitThen(() -> t != null);
		else
			condition.satisfyAll(() -> t = source.source());
		return t;
	}

}
