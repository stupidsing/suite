package suite.streamlet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Predicate;

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

	public static <T> Reactive<T> append(Reactive<T> r0, Reactive<T> r1) {
		Bag<Sink<T>> sinks = new Bag<>();
		Sink<T> sink = t -> sinkAll(sinks, t);
		r0.sink(sink);
		r1.sink(sink);
		return from(sinks);
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

	public <U> Reactive<U> concatMap(Fun<T, Reactive<U>> fun) {
		Bag<Sink<U>> sinks = new Bag<>();
		sink.sink(t -> fun.apply(t).sink(u -> sinkAll(sinks, u)));
		return from(sinks);
	}

	public Reactive<T> delay(int milliseconds) {
		Bag<Sink<T>> sinks = new Bag<>();
		sink.sink(t -> executor.schedule(() -> sinkAll(sinks, t), milliseconds, TimeUnit.MILLISECONDS));
		return from(sinks);
	}

	public Reactive<T> filter(Predicate<T> pred) {
		Bag<Sink<T>> sinks = new Bag<>();
		sink.sink(t -> {
			if (pred.test(t))
				sinkAll(sinks, t);
		});
		return from(sinks);
	}

	public <U> Reactive<U> fold(U init, BiFunction<U, T, U> fun) {
		Bag<Sink<U>> sinks = new Bag<>();
		CasReference<U> cr = new CasReference<>(init);
		sink.sink(t1 -> sinkAll(sinks, cr.apply(t0 -> fun.apply(t0, t1))));
		return from(sinks);
	}

	public <U> Reactive<U> map(Fun<T, U> fun) {
		Bag<Sink<U>> sinks = new Bag<>();
		sink.sink(t -> sinkAll(sinks, fun.apply(t)));
		return from(sinks);
	}

	public Reactive<T> resample(Reactive<?> event) {
		List<T> ts = new ArrayList<>();
		ts.add(null);
		sink.sink(t -> ts.set(0, t));

		Bag<Sink<T>> sinks = new Bag<>();
		event.sink(e -> sinkAll(sinks, ts.get(0)));
		return from(sinks);
	}

	public void sink(Sink<T> sink_) {
		sink.sink(sink_);
	}

	public Streamlet<T> streamlet() {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();
		sink.sink(queue::offerQuietly);
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

}
