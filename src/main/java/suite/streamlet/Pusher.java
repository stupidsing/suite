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
 * A push-based functional reactive programming class.
 * 
 * @author ywsing
 */
public class Pusher<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Bag<Sink<T>> pushees;

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Sink<T1> sink);
	}

	public static <T> Pusher<T> append(Pusher<T> n0, Pusher<T> n1) {
		return of(push -> {
			n0.wire_(push);
			n1.wire_(push);
		});
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
		return of(push -> {
			var cr = new CasReference<Pair<T, U>>(Pair.of(null, null));
			Sink<Pair<T, U>> recalc = pair -> push.f(pair.map(fun));
			n0.wire_(t -> recalc.f(cr.apply(pair -> Pair.of(t, pair.t1))));
			n1.wire_(u -> recalc.f(cr.apply(pair -> Pair.of(pair.t0, u))));
		});
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
		this(new Bag<>());
	}

	private Pusher(Bag<Sink<T>> pushees) {
		this.pushees = pushees;
	}

	public <U> Pusher<U> concatMap(Fun<T, Pusher<U>> fun) {
		return redirect_((t, push) -> fun.apply(t).wire_(push));
	}

	public Pusher<T> delay(int ms) {
		return redirect_((t, push) -> executor.schedule(() -> push.f(t), ms, TimeUnit.MILLISECONDS));
	}

	public Pusher<T> delayAccum(int ms) {
		var al = new AtomicLong();
		return redirect_((t, push) -> {
			var current = System.currentTimeMillis();
			al.set(current);
			executor.schedule(() -> {
				if (al.get() == current)
					push.f(t);
			}, ms, TimeUnit.MILLISECONDS);
		});
	}

	public Pusher<T> edge() {
		return redirect_(new Redirector<>() {
			private T previous = null;

			public void accept(T t, Sink<T> push) {
				if (previous == null || !Objects.equals(previous, t))
					push.f(t);
			}
		});
	}

	public Pusher<T> filter(Predicate<T> pred) {
		return redirect_((t, push) -> {
			if (pred.test(t))
				push.f(t);
		});
	}

	public <U> Pusher<U> fold(U init, Fun2<U, T, U> fun) {
		var cr = new CasReference<U>(init);
		return redirect_((t1, push) -> push.f(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public Pusher<T> level(int ms) {
		return resample(ofFixed(ms));
	}

	public <U> Pusher<U> map(Fun<T, U> fun) {
		return redirect_((t, sink) -> sink.f(fun.apply(t)));
	}

	public void push(T t) {
		pushees.forEach(sink -> sink.f(t));
	}

	public Puller<T> pushee() {
		var queue = new NullableSyncQueue<T>();
		wire_(queue::offerQuietly);
		return Puller.of(() -> rethrow(queue::take));
	}

	public <U> Pusher<U> redirect(Redirector<T, U> redirector) {
		return redirect_(redirector);
	}

	public Pusher<T> resample(Pusher<?> event) {
		var mut = Mutable.<T> nil();
		wire_(mut::update);
		return event.redirect_((e, push) -> push.f(mut.value()));
	}

	public Pusher<T> unique() {
		var set = new HashSet<>();
		return redirect_((t, push) -> {
			if (set.add(t))
				push.f(t);
		});
	}

	public void wire(Runnable runner) {
		wire_(t -> runner.run());
	}

	public void wire(Sink<T> pushee) {
		wire_(pushee);
	}

	private <U> Pusher<U> redirect_(Redirector<T, U> redirector) {
		return of(push -> wire_(t -> redirector.accept(t, push)));
	}

	private Runnable wire_(Sink<T> pushee) {
		pushees.add(pushee);
		return () -> pushees.remove(pushee);
	}

}
