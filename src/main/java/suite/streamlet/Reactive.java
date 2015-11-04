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

	private Bag<Sink<T>> receivers;

	public static <T> Reactive<T> append(Reactive<T> r0, Reactive<T> r1) {
		Reactive<T> reactive1 = new Reactive<>();
		Sink<T> sink = reactive1::fire;
		r0.register(sink);
		r1.register(sink);
		return reactive1;
	}

	public static <T> Reactive<T> from(Source<T> source) {
		Reactive<T> reactive1 = new Reactive<>();
		executor.submit(() -> {
			T t;
			while ((t = source.source()) != null)
				reactive1.fire(t);
		});
		return reactive1;
	}

	public static <T, U, V> Reactive<V> merge(Reactive<T> r0, Reactive<U> r1, BiFunction<T, U, V> fun) {
		Reactive<V> reactive1 = new Reactive<>();
		CasReference<Pair<T, U>> cr = new CasReference<>(Pair.of(null, null));
		Sink<Pair<T, U>> recalc = pair -> reactive1.fire(fun.apply(pair.t0, pair.t1));
		r0.register(t -> recalc.sink(cr.apply(pair -> Pair.of(t, pair.t1))));
		r1.register(u -> recalc.sink(cr.apply(pair -> Pair.of(pair.t0, u))));
		return reactive1;
	}

	public Reactive() {
		this(new Bag<>());
	}

	private Reactive(Bag<Sink<T>> receivers) {
		this.receivers = receivers;
	}

	public <U> Reactive<U> concatMap(Fun<T, Reactive<U>> fun) {
		Reactive<U> reactive1 = new Reactive<>();
		register(t -> fun.apply(t).register(reactive1::fire));
		return reactive1;
	}

	public Reactive<T> delay(int milliseconds) {
		Reactive<T> reactive1 = new Reactive<>();
		register(t -> executor.schedule(() -> reactive1.fire(t), milliseconds, TimeUnit.MILLISECONDS));
		return reactive1;
	}

	public Reactive<T> filter(Predicate<T> pred) {
		Reactive<T> reactive1 = new Reactive<>();
		register(t -> {
			if (pred.test(t))
				reactive1.fire(t);
		});
		return reactive1;
	}

	public void fire(T t) {
		receivers.forEach(sink -> sink.sink(t));
	}

	public <U> Reactive<U> fold(U init, BiFunction<U, T, U> fun) {
		Reactive<U> reactive1 = new Reactive<>();
		CasReference<U> cr = new CasReference<>(init);
		register(t1 -> reactive1.fire(cr.apply(t0 -> fun.apply(t0, t1))));
		return reactive1;
	}

	public Outlet<T> outlet() {
		NullableSynchronousQueue<T> queue = new NullableSynchronousQueue<>();
		register(queue::offerQuietly);
		return new Outlet<>(() -> {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	public <U> Reactive<U> map(Fun<T, U> fun) {
		Reactive<U> reactive1 = new Reactive<>();
		register(t -> reactive1.fire(fun.apply(t)));
		return reactive1;
	}

	public Reactive<T> resample(Reactive<?> event) {
		List<T> ts = new ArrayList<>();
		ts.add(null);
		register(t -> ts.set(0, t));

		Reactive<T> reactive1 = new Reactive<>();
		event.register(e -> reactive1.fire(ts.get(0)));
		return reactive1;
	}

	public void register(Sink<T> receiver) {
		register(receiver);
	}

	public Reactive<T> unique() {
		Reactive<T> reactive1 = new Reactive<>();
		register(new Sink<T>() {
			private T previous = null;

			public void sink(T t) {
				if (previous == null || !previous.equals(t))
					reactive1.fire(t);
			}
		});
		return reactive1;
	}

}
