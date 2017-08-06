package suite.streamlet;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import suite.adt.pair.Pair;
import suite.concurrent.Bag;
import suite.concurrent.CasReference;
import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Sink;
import suite.util.FunUtil.Source;
import suite.util.FunUtil2.Fun2;
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
		public void accept(T0 t0, Sink<T1> sink);
	}

	public static <T> Nerve<T> append(Nerve<T> n0, Nerve<T> n1) {
		return of(fire -> {
			n0.register(fire);
			n1.register(fire);
		});
	}

	public static <T> Nerve<T> from(Source<T> source) {
		return of(fire -> executor.submit(() -> {
			T t;
			while ((t = source.source()) != null)
				fire.sink(t);
		}));
	}

	public static <T, U, V> Nerve<V> merge(Nerve<T> n0, Nerve<U> n1, Fun2<T, U, V> fun) {
		return of(fire -> {
			CasReference<Pair<T, U>> cr = new CasReference<>(Pair.of(null, null));
			Sink<Pair<T, U>> recalc = pair -> fire.sink(fun.apply(pair.t0, pair.t1));
			n0.register(t -> recalc.sink(cr.apply(pair -> Pair.of(t, pair.t1))));
			n1.register(u -> recalc.sink(cr.apply(pair -> Pair.of(pair.t0, u))));
		});
	}

	public static <T> Nerve<T> of(Sink<Sink<T>> sink) {
		Nerve<T> nerve = of();
		sink.sink(nerve::fire);
		return nerve;
	}

	public static <T> Nerve<T> of() {
		return new Nerve<>();
	}

	private Nerve() {
		this(new Bag<>());
	}

	private Nerve(Bag<Sink<T>> receivers) {
		this.receivers = receivers;
	}

	public void close() {
		receivers = new Bag<>();
	}

	public <U> Nerve<U> concatMap(Fun<T, Nerve<U>> fun) {
		return redirect((t, fire) -> fun.apply(t).register(fire));
	}

	public Nerve<T> delay(int milliseconds) {
		return redirect((t, fire) -> executor.schedule(() -> fire.sink(t), milliseconds, TimeUnit.MILLISECONDS));
	}

	public Nerve<T> edge() {
		return redirect(new Redirector<T, T>() {
			private T previous = null;

			public void accept(T t, Sink<T> fire) {
				if (previous == null || !previous.equals(t))
					fire.sink(t);
			}
		});
	}

	public Nerve<T> filter(Predicate<T> pred) {
		return redirect((t, fire) -> {
			if (pred.test(t))
				fire.sink(t);
		});
	}

	public void fire(T t) {
		receivers.forEach(sink -> sink.sink(t));
	}

	public <U> Nerve<U> fold(U init, Fun2<U, T, U> fun) {
		CasReference<U> cr = new CasReference<>(init);
		return redirect((t1, fire) -> fire.sink(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public <U> Nerve<U> map(Fun<T, U> fun) {
		return redirect((t, sink) -> sink.sink(fun.apply(t)));
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
		Nerve<U> nerve1 = of();
		register(t -> redirector.accept(t, nerve1::fire));
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
		return event.redirect((e, fire) -> fire.sink(ts.get(0)));
	}

	public Nerve<T> unique() {
		Set<T> set = new HashSet<>();
		return redirect((t, fire) -> {
			if (set.add(t))
				fire.sink(t);
		});
	}

}
