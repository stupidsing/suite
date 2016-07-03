package suite.streamlet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Reactive<T1> reactive);
	}

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

	public void close() {
		receivers = new Bag<>();
	}

	public <U> Reactive<U> concatMap(Fun<T, Reactive<U>> fun) {
		return redirect((t, reactive1) -> fun.apply(t).register(reactive1::fire));
	}

	public Reactive<T> delay(int milliseconds) {
		return redirect((t, reactive1) -> executor.schedule(() -> reactive1.fire(t), milliseconds, TimeUnit.MILLISECONDS));
	}

	public Reactive<T> edge() {
		return redirect(new Redirector<T, T>() {
			private T previous = null;

			public void accept(T t, Reactive<T> reactive1) {
				if (previous == null || !previous.equals(t))
					reactive1.fire(t);
			}
		});
	}

	public Reactive<T> filter(Predicate<T> pred) {
		return redirect((t, reactive1) -> {
			if (pred.test(t))
				reactive1.fire(t);
		});
	}

	public void fire(T t) {
		receivers.forEach(sink -> sink.sink(t));
	}

	public <U> Reactive<U> fold(U init, BiFunction<U, T, U> fun) {
		CasReference<U> cr = new CasReference<>(init);
		return redirect((t1, reactive1) -> reactive1.fire(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public <U> Reactive<U> map(Fun<T, U> fun) {
		return redirect((t, reactive1) -> reactive1.fire(fun.apply(t)));
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

	public <U> Reactive<U> redirect(Redirector<T, U> redirector) {
		Reactive<U> reactive1 = new Reactive<>();
		register(t -> redirector.accept(t, reactive1));
		return reactive1;
	}

	public void register(Runnable receiver) {
		receivers.add(dummy -> receiver.run());
	}

	public void register(Sink<T> receiver) {
		receivers.add(receiver);
	}

	public Reactive<T> resample(Reactive<?> event) {
		List<T> ts = new ArrayList<>();
		ts.add(null);
		register(t -> ts.set(0, t));
		return event.redirect((e, reactive1) -> reactive1.fire(ts.get(0)));
	}

	public Reactive<T> unique() {
		Set<T> set = new HashSet<>();
		return redirect((t, reactive1) -> {
			if (set.add(t))
				reactive1.fire(t);
		});
	}

}
