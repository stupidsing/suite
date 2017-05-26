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

import suite.adt.pair.Pair;
import suite.concurrent.Bag;
import suite.concurrent.CasReference;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.NullableSyncQueue;

/**
 * A pull-based functional reactive programming class.
 * 
 * @author ywsing
 */
public class Nerve<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Bag<Sink<T>> receivers;

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Nerve<T1> nerve);
	}

	public static <T> Nerve<T> append(Nerve<T> n0, Nerve<T> n1) {
		Nerve<T> nerve1 = new Nerve<>();
		Sink<T> sink = nerve1::fire;
		n0.register(sink);
		n1.register(sink);
		return nerve1;
	}

	public static <T> Nerve<T> from(Source<T> source) {
		Nerve<T> nerve1 = new Nerve<>();
		executor.submit(() -> {
			T t;
			while ((t = source.source()) != null)
				nerve1.fire(t);
		});
		return nerve1;
	}

	public static <T, U, V> Nerve<V> merge(Nerve<T> n0, Nerve<U> n1, BiFunction<T, U, V> fun) {
		Nerve<V> nerve1 = new Nerve<>();
		CasReference<Pair<T, U>> cr = new CasReference<>(Pair.of(null, null));
		Sink<Pair<T, U>> recalc = pair -> nerve1.fire(fun.apply(pair.t0, pair.t1));
		n0.register(t -> recalc.sink(cr.apply(pair -> Pair.of(t, pair.t1))));
		n1.register(u -> recalc.sink(cr.apply(pair -> Pair.of(pair.t0, u))));
		return nerve1;
	}

	public Nerve() {
		this(new Bag<>());
	}

	private Nerve(Bag<Sink<T>> receivers) {
		this.receivers = receivers;
	}

	public void close() {
		receivers = new Bag<>();
	}

	public <U> Nerve<U> concatMap(Fun<T, Nerve<U>> fun) {
		return redirect((t, nerve1) -> fun.apply(t).register(nerve1::fire));
	}

	public Nerve<T> delay(int milliseconds) {
		return redirect((t, nerve1) -> executor.schedule(() -> nerve1.fire(t), milliseconds, TimeUnit.MILLISECONDS));
	}

	public Nerve<T> edge() {
		return redirect(new Redirector<T, T>() {
			private T previous = null;

			public void accept(T t, Nerve<T> nerve1) {
				if (previous == null || !previous.equals(t))
					nerve1.fire(t);
			}
		});
	}

	public Nerve<T> filter(Predicate<T> pred) {
		return redirect((t, nerve1) -> {
			if (pred.test(t))
				nerve1.fire(t);
		});
	}

	public void fire(T t) {
		receivers.forEach(sink -> sink.sink(t));
	}

	public <U> Nerve<U> fold(U init, BiFunction<U, T, U> fun) {
		CasReference<U> cr = new CasReference<>(init);
		return redirect((t1, nerve1) -> nerve1.fire(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public <U> Nerve<U> map(Fun<T, U> fun) {
		return redirect((t, nerve1) -> nerve1.fire(fun.apply(t)));
	}

	public Outlet<T> outlet() {
		NullableSyncQueue<T> queue = new NullableSyncQueue<>();
		register(queue::offerQuietly);
		return Outlet.of(() -> {
			try {
				return queue.take();
			} catch (InterruptedException ex) {
				throw new RuntimeException(ex);
			}
		});
	}

	public <U> Nerve<U> redirect(Redirector<T, U> redirector) {
		Nerve<U> nerve1 = new Nerve<>();
		register(t -> redirector.accept(t, nerve1));
		return nerve1;
	}

	public void register(Runnable receiver) {
		receivers.add(dummy -> receiver.run());
	}

	public void register(Sink<T> receiver) {
		receivers.add(receiver);
	}

	public Nerve<T> resample(Nerve<?> event) {
		List<T> ts = new ArrayList<>();
		ts.add(null);
		register(t -> ts.set(0, t));
		return event.redirect((e, nerve1) -> nerve1.fire(ts.get(0)));
	}

	public Nerve<T> unique() {
		Set<T> set = new HashSet<>();
		return redirect((t, nerve1) -> {
			if (set.add(t))
				nerve1.fire(t);
		});
	}

}
