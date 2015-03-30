package suite.streamlet;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import suite.concurrent.Bag;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.NullableSynchronousQueue;

public class Reactive<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Sink<Sink<T>> sink;

	private Reactive(Sink<Sink<T>> sink) {
		this.sink = sink;
	}

	public static <T> Reactive<T> from(Source<T> source) {
		Bag<Sink<T>> sinks = new Bag<>();
		executor.submit(() -> {
			T t;
			while ((t = source.source()) != null)
				sinkAll(sinks, t);
		});
		return from(sinks);
	}

	private static <T> Reactive<T> from(Bag<Sink<T>> sinks) {
		return from(sink -> sinks.add(sink));
	}

	public static <T> Reactive<T> from(Sink<Sink<T>> sink) {
		return new Reactive<>(sink);
	}

	public Reactive<T> delay(int milliseconds) {
		Bag<Sink<T>> sinks = new Bag<>();
		sink.sink(t -> executor.schedule(() -> sinkAll(sinks, t), milliseconds, TimeUnit.MILLISECONDS));
		return from(sinks);
	}

	public <U> Reactive<U> map(Fun<T, U> fun) {
		Bag<Sink<U>> sinks = new Bag<>();
		sink.sink(t -> sinkAll(sinks, t != null ? fun.apply(t) : null));
		return from(sinks);
	}

	public Streamlet<T> streamlet() {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();
		sink.sink(t -> queue.offerQuietly(t));
		return Read.from(() -> {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	private static <T> void sinkAll(Bag<Sink<T>> bag, T t) {
		bag.forEach(sink -> sink.sink(t));
	}

}
