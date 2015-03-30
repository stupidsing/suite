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

	private static class SinkBag<T> extends Bag<Sink<T>> {
		public void sinkAll(T t) {
			forEach(sink -> sink.sink(t));
		}
	}

	private Reactive(Sink<Sink<T>> sink) {
		this.sink = sink;
	}

	public static <T> Reactive<T> from(Source<T> source) {
		SinkBag<T> sinks = new SinkBag<>();
		new Thread() {
			public void run() {
				T t;
				while ((t = source.source()) != null)
					sinks.sinkAll(t);
			}
		}.start();
		return from(sinks);
	}

	private static <T> Reactive<T> from(Bag<Sink<T>> sinks) {
		return from(sink -> sinks.add(sink));
	}

	public static <T> Reactive<T> from(Sink<Sink<T>> sink) {
		return new Reactive<>(sink);
	}

	public Reactive<T> delay(int milliseconds) {
		SinkBag<T> sinks = new SinkBag<>();
		sink.sink(t -> executor.schedule(() -> sinks.sinkAll(t), milliseconds, TimeUnit.MILLISECONDS));
		return from(sinks);
	}

	public <U> Reactive<U> map(Fun<T, U> fun) {
		SinkBag<U> sinks = new SinkBag<>();
		sink.sink(t -> sinks.sinkAll(t != null ? fun.apply(t) : null));
		return from(sinks);
	}

	public Source<T> source() {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();
		sink.sink(t -> queue.offerQuietly(t));

		return () -> {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		};
	}

}
