package suite.streamlet;

import static primal.statics.Rethrow.ex;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import primal.Ob;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Sink;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import primal.adt.Mutable;
import suite.concurrent.Bag;
import suite.concurrent.CasReference;
import suite.util.NullableSyncQueue;

/**
 * A push-based functional reactive programming class.
 * 
 * @author ywsing
 */
public class Pusher0<T> {

	private static ScheduledExecutorService executor = Executors.newScheduledThreadPool(8);

	private Bag<Sink<T>> pushees;

	public interface Redirector<T0, T1> {
		public void accept(T0 t0, Sink<T1> sink);
	}

	public static <T> Pusher0<T> append(Pusher0<T> n0, Pusher0<T> n1) {
		var pusher = new Pusher0<T>();
		n0.wire_(pusher::push);
		n1.wire_(pusher::push);
		return pusher;
	}

	public static <T> Pusher0<T> from(Source<T> source) {
		var pusher = new Pusher0<T>();
		executor.submit(() -> {
			T t;
			while ((t = source.g()) != null)
				pusher.push(t);
		});
		return pusher;
	}

	public static <T> void loop(Source<T> source, Sink<Pusher0<T>> sink) {
		var pusher = new Pusher0<T>();
		T t;

		executor.submit(() -> sink.f(pusher));

		while ((t = source.g()) != null)
			pusher.push(t);
	}

	public static <T, U, V> Pusher0<V> merge(Pusher0<T> n0, Pusher0<U> n1, Fun2<T, U, V> fun) {
		var pusher = new Pusher0<V>();
		var cr = new CasReference<Pair<T, U>>(Pair.of(null, null));
		Sink<Pair<T, U>> recalc = pair -> pusher.push(pair.map(fun));
		n0.wire_(t -> recalc.f(cr.apply(pair -> Pair.of(t, pair.v))));
		n1.wire_(u -> recalc.f(cr.apply(pair -> Pair.of(pair.k, u))));
		return pusher;
	}

	public static Pusher0<Object> ofFixed(int ms) {
		var pusher = new Pusher0<Object>();
		executor.scheduleAtFixedRate(() -> pusher.push(null), ms, ms, TimeUnit.MILLISECONDS);
		return pusher;
	}

	private Pusher0() {
		this(new Bag<>());
	}

	private Pusher0(Bag<Sink<T>> pushees) {
		this.pushees = pushees;
	}

	public <U> Pusher0<U> concatMap(Fun<T, Pusher0<U>> fun) {
		return redirect_((t, push) -> fun.apply(t).wire_(push));
	}

	public Pusher0<T> delay(int ms) {
		return redirect_((t, push) -> executor.schedule(() -> push.f(t), ms, TimeUnit.MILLISECONDS));
	}

	public Pusher0<T> delayAccum(int ms) {
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

	public Pusher0<T> edge() {
		return redirect_(new Redirector<>() {
			private T previous = null;

			public void accept(T t, Sink<T> push) {
				if (previous == null || !Ob.equals(previous, t))
					push.f(t);
			}
		});
	}

	public Pusher0<T> filter(Predicate<T> pred) {
		return redirect_((t, push) -> {
			if (pred.test(t))
				push.f(t);
		});
	}

	public <U> Pusher0<U> fold(U init, Fun2<U, T, U> fun) {
		var cr = new CasReference<U>(init);
		return redirect_((t1, push) -> push.f(cr.apply(t0 -> fun.apply(t0, t1))));
	}

	public Pusher0<T> level(int ms) {
		return resample(ofFixed(ms));
	}

	public <U> Pusher0<U> map(Fun<T, U> fun) {
		return redirect_((t, sink) -> sink.f(fun.apply(t)));
	}

	public void push(T t) {
		pushees.forEach(sink -> sink.f(t));
	}

	public Puller<T> pushee() {
		var queue = new NullableSyncQueue<T>();
		wire_(queue::offerQuietly);
		return Puller.of(() -> ex(queue::take));
	}

	public <U> Pusher0<U> redirect(Redirector<T, U> redirector) {
		return redirect_(redirector);
	}

	public Pusher0<T> resample(Pusher0<?> event) {
		var mut = Mutable.<T> nil();
		wire_(mut::update);
		return event.redirect_((e, push) -> push.f(mut.value()));
	}

	public Pusher0<T> unique() {
		var set = new HashSet<>();
		return redirect_((t, push) -> {
			if (set.add(t))
				push.f(t);
		});
	}

	public Runnable wire(Runnable runner) {
		return wire_(t -> runner.run());
	}

	public Runnable wire(Sink<T> pushee) {
		return wire_(pushee);
	}

	private <U> Pusher0<U> redirect_(Redirector<T, U> redirector) {
		var pusher = new Pusher0<U>();
		wire_(t -> redirector.accept(t, pusher::push));
		return pusher;
	}

	private Runnable wire_(Sink<T> pushee) {
		pushees.add(pushee);
		return () -> pushees.remove(pushee);
	}

}
