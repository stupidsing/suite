package suite.streamlet;

import static suite.util.Friends.rethrow;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import suite.adt.Mutable;
import suite.adt.pair.Pair;
import suite.concurrent.CasReference;
import suite.streamlet.FunUtil.Fun;
import suite.streamlet.FunUtil.Sink;
import suite.streamlet.FunUtil.Source;
import suite.streamlet.FunUtil2.Fun2;
import suite.util.NullableSyncQueue;

/**
 * A push-based functional reactive programming class.
 * 
 * @author ywsing
 */
public class Pusher<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private WeakHashMap<Object, List<Sink<T>>> pushees;

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Pusher<T1> sink);
	}

	public static <T> Pusher<T> append(Pusher<T> n0, Pusher<T> n1) {
		Pusher<T> pusher = of();
		n0.wire_(pusher, pusher::push);
		n1.wire_(pusher, pusher::push);
		return pusher;
	}

	public static <T> Pusher<T> from(Source<T> source) {
		return of(sink -> executor.submit(() -> {
			T t;
			while ((t = source.g()) != null)
				sink.f(t);
		}));
	}

	public static <T> void loop(Source<T> source, Sink<Pusher<T>> sink) {
		Pusher<T> pusher = of();
		T t;

		executor.submit(() -> sink.f(pusher));

		while ((t = source.g()) != null)
			pusher.push(t);
	}

	public static <T, U, V> Pusher<V> merge(Pusher<T> n0, Pusher<U> n1, Fun2<T, U, V> fun) {
		Pusher<V> pusher = of();
		var cr = new CasReference<Pair<T, U>>(Pair.of(null, null));
		Sink<Pair<T, U>> recalc = pair -> pusher.push(pair.map(fun));
		n0.wire_(pusher, t -> recalc.f(cr.apply(pair -> Pair.of(t, pair.t1))));
		n1.wire_(pusher, u -> recalc.f(cr.apply(pair -> Pair.of(pair.t0, u))));
		return pusher;
	}

	public static Pusher<Object> ofFixed(int ms) {
		return of(push -> executor.scheduleAtFixedRate(() -> push.f(null), ms, ms, TimeUnit.MILLISECONDS));
	}

	public static <T> Pusher<T> of(Sink<Sink<T>> sink) {
		Pusher<T> pusher = of();
		sink.f(pusher::push);
		return pusher;
	}

	public static <T> Pusher<T> of() {
		return new Pusher<>();
	}

	private Pusher() {
		this(new WeakHashMap<>());
	}

	private Pusher(WeakHashMap<Object, List<Sink<T>>> pushees) {
		this.pushees = pushees;
	}

	public <U> Pusher<U> concatMap(Fun<T, Pusher<U>> fun) {
		return redirect_((t, pusher) -> fun.apply(t).wire_(pusher, pusher::push));
	}

	public Pusher<T> delay(int ms) {
		return redirect_((t, pusher) -> executor.schedule(() -> pusher.push(t), ms, TimeUnit.MILLISECONDS));
	}

	public Pusher<T> delayAccum(int ms) {
		var al = new AtomicLong();
		return redirect_((t, pusher) -> {
			var current = System.currentTimeMillis();
			al.set(current);
			executor.schedule(() -> {
				if (al.get() == current)
					pusher.push(t);
			}, ms, TimeUnit.MILLISECONDS);
		});
	}

	public Pusher<T> edge() {
		return redirect_(new Redirector<>() {
			private T previous = null;

			public void accept(T t, Pusher<T> pusher) {
				if (previous == null || !Objects.equals(previous, t))
					pusher.push(t);
			}
		});
	}

	public Pusher<T> filter(Predicate<T> pred) {
		return redirect_((t, pusher) -> {
			if (pred.test(t))
				pusher.push(t);
		});
	}

	public <U> Pusher<U> fold(U init, Fun2<U, T, U> fun) {
		var cr = new CasReference<U>(init);
		return redirect_((t1, pusher) -> pusher.push(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public Pusher<T> level(int ms) {
		return resample(ofFixed(ms));
	}

	public <U> Pusher<U> map(Fun<T, U> fun) {
		return redirect_((t, pusher) -> pusher.push(fun.apply(t)));
	}

	public void push(T t) {
		pushees.forEach((gc, sinks) -> sinks.forEach(sink -> sink.f(t)));
	}

	public Puller<T> pushee() {
		var queue = new NullableSyncQueue<T>();
		wire_(queue, queue::offerQuietly);
		return Puller.of(() -> rethrow(queue::take));
	}

	public <U> Pusher<U> redirect(Redirector<T, U> redirector) {
		return redirect_(redirector);
	}

	public Pusher<T> resample(Pusher<?> event) {
		var mut = Mutable.<T> nil();
		wire_(mut, mut::update);
		return event.redirect_((e, pusher) -> pusher.push(mut.value()));
	}

	public Pusher<T> unique() {
		var set = new HashSet<>();
		return redirect_((t, pusher) -> {
			if (set.add(t))
				pusher.push(t);
		});
	}

	public void wire(Object gc, Runnable runner) {
		wire_(gc, t -> runner.run());
	}

	public void wire(Object gc, Sink<T> pushee) {
		wire_(gc, pushee);
	}

	private <U> Pusher<U> redirect_(Redirector<T, U> redirector) {
		Pusher<U> pusher = of();
		wire_(pusher, t -> redirector.accept(t, pusher));
		return pusher;
	}

	private void wire_(Object gc, Sink<T> pushee) {
		pushees.computeIfAbsent(gc, o -> new ArrayList<>()).add(pushee);
	}

}
