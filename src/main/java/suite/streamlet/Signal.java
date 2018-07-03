package suite.streamlet;

import static suite.util.Friends.rethrow;

import java.util.HashSet;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.concurrent.Bag;
import suite.concurrent.CasReference;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.util.NullableSyncQueue;

/**
 * A pull-based functional reactive programming class.
 * 
 * @author ywsing
 */
public class Signal<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Bag<Sink<T>> receivers;

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Sink<T1> sink);
	}

	public static <T> Signal<T> append(Signal<T> n0, Signal<T> n1) {
		return of(fire -> {
			n0.wire_(fire);
			n1.wire_(fire);
		});
	}

	public static <T> Signal<T> from(Source<T> source) {
		return of(fire -> executor.submit(() -> {
			T t;
			while ((t = source.source()) != null)
				fire.sink(t);
		}));
	}

	public static <T> void loop(Source<T> source, Sink<Signal<T>> sink) {
		Signal<T> signal = of();
		T t;

		executor.submit(() -> sink.sink(signal));

		while ((t = source.source()) != null)
			signal.fire(t);
	}

	public static <T, U, V> Signal<V> merge(Signal<T> n0, Signal<U> n1, Fun2<T, U, V> fun) {
		return of(fire -> {
			CasReference<Pair<T, U>> cr = new CasReference<>(Pair.of(null, null));
			Sink<Pair<T, U>> recalc = pair -> fire.sink(fun.apply(pair.t0, pair.t1));
			n0.wire_(t -> recalc.sink(cr.apply(pair -> Pair.of(t, pair.t1))));
			n1.wire_(u -> recalc.sink(cr.apply(pair -> Pair.of(pair.t0, u))));
		});
	}

	public static Signal<Object> ofFixed(int ms) {
		return of(fire -> executor.scheduleAtFixedRate(() -> fire.sink(null), ms, ms, TimeUnit.MILLISECONDS));
	}

	public static <T> Signal<T> of(Sink<Sink<T>> sink) {
		Signal<T> signal = of();
		sink.sink(signal::fire);
		return signal;
	}

	public static <T> Signal<T> of() {
		return new Signal<>();
	}

	private Signal() {
		this(new Bag<>());
	}

	private Signal(Bag<Sink<T>> receivers) {
		this.receivers = receivers;
	}

	public <U> Signal<U> concatMap(Fun<T, Signal<U>> fun) {
		return redirect_((t, fire) -> fun.apply(t).wire_(fire));
	}

	public Signal<T> delay(int ms) {
		return redirect_((t, fire) -> executor.schedule(() -> fire.sink(t), ms, TimeUnit.MILLISECONDS));
	}

	public Signal<T> delayAccum(int ms) {
		var al = new AtomicLong();
		return redirect_((t, fire) -> {
			var current = System.currentTimeMillis();
			al.set(current);
			executor.schedule(() -> {
				if (al.get() == current)
					fire.sink(t);
			}, ms, TimeUnit.MILLISECONDS);
		});
	}

	public Signal<T> edge() {
		return redirect_(new Redirector<>() {
			private T previous = null;

			public void accept(T t, Sink<T> fire) {
				if (previous == null || !Objects.equals(previous, t))
					fire.sink(t);
			}
		});
	}

	public Signal<T> filter(Predicate<T> pred) {
		return redirect_((t, fire) -> {
			if (pred.test(t))
				fire.sink(t);
		});
	}

	public void fire(T t) {
		receivers.forEach(sink -> sink.sink(t));
	}

	public <U> Signal<U> fold(U init, Fun2<U, T, U> fun) {
		CasReference<U> cr = new CasReference<>(init);
		return redirect_((t1, fire) -> fire.sink(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public Signal<T> level(int ms) {
		return resample(ofFixed(ms));
	}

	public <U> Signal<U> map(Fun<T, U> fun) {
		return redirect_((t, sink) -> sink.sink(fun.apply(t)));
	}

	public Outlet<T> outlet() {
		var queue = new NullableSyncQueue<T>();
		wire_(queue::offerQuietly);
		return Outlet.of(() -> rethrow(queue::take));
	}

	public <U> Signal<U> redirect(Redirector<T, U> redirector) {
		return redirect_(redirector);
	}

	public Signal<T> resample(Signal<?> event) {
		var mut = Mutable.<T> nil();
		wire_(mut::update);
		return event.redirect_((e, fire) -> fire.sink(mut.get()));
	}

	public Signal<T> unique() {
		var set = new HashSet<>();
		return redirect_((t, fire) -> {
			if (set.add(t))
				fire.sink(t);
		});
	}

	public void wire(Runnable receiver) {
		wire_(t -> receiver.run());
	}

	public void wire(Sink<T> receiver) {
		wire_(receiver);
	}

	private <U> Signal<U> redirect_(Redirector<T, U> redirector) {
		return of(fire -> wire_(t -> redirector.accept(t, fire)));
	}

	private void wire_(Sink<T> receiver) {
		receivers.add(receiver);
	}

}
