package suite.streamlet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import suite.adt.Pair;
import suite.concurrent.Bag;
import suite.concurrent.CasReference;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.NullableSynchronousQueue;

/**
 * A pull-based functional reactive programming class.
 * 
 * @author ywsing
 */
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

	public static <T, U, V> Reactive<V> merge(Reactive<T> r0, Reactive<U> r1, BiFunction<T, U, V> fun) {
		Bag<Sink<V>> sinks = new Bag<>();
		CasReference<Pair<T, U>> cr = new CasReference<>(Pair.of(null, null));
		Sink<Pair<T, U>> recalc = pair -> sinkAll(sinks, fun.apply(pair.t0, pair.t1));
		r0.sink.sink(t -> recalc.sink(cr.apply(pair -> Pair.of(t, pair.t1))));
		r1.sink.sink(u -> recalc.sink(cr.apply(pair -> Pair.of(pair.t0, u))));
		return from(sinks);
	}

	public Reactive<T> delay(int milliseconds) {
		Bag<Sink<T>> sinks = new Bag<>();
		sink.sink(t -> executor.schedule(() -> sinkAll(sinks, t), milliseconds, TimeUnit.MILLISECONDS));
		return from(sinks);
	}

	public Reactive<T> fold(T init, BiFunction<T, T, T> fun) {
		Bag<Sink<T>> sinks = new Bag<>();
		CasReference<T> cr = new CasReference<>(init);
		sink.sink(t1 -> sinkAll(sinks, cr.apply(t0 -> fun.apply(t0, t1))));
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

	public Reactive<T> unique() {
		Bag<Sink<T>> sinks = new Bag<>();
		sink.sink(new Sink<T>() {
			private T previous = null;

			public void sink(T t) {
				if (previous == null || !previous.equals(t))
					sinkAll(sinks, t);
			}
		});
		return from(sinks);
	}

	private static <T> void sinkAll(Bag<Sink<T>> bag, T t) {
		bag.forEach(sink -> sink.sink(t));
	}

	private static <T> List<T> ref(T t) {
		List<T> list = new ArrayList<>();
		list.add(t);
		return list;
	}

}
