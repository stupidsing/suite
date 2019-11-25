package suite.util;

import static primal.statics.Fail.fail;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import primal.Verbs.Wait;
import primal.adt.Pair;
import primal.fp.Funs.Fun;
import primal.fp.Funs.Source;
import primal.fp.Funs2.Fun2;
import suite.adt.PriorityQueue;

public class Memoize {

	private enum State {
		EMPTY__, INUSE_, FLAGGED,
	}

	/**
	 * Cache results of a two-parameters function call, no clean-up.
	 */
	public static <I0, I1, O> Fun2<I0, I1, O> biFunction(Fun2<I0, I1, O> fun) {
		var results = new ConcurrentHashMap<Pair<I0, I1>, O>();
		return (in0, in1) -> results.computeIfAbsent(Pair.of(in0, in1), p -> fun.apply(in0, in1));
	}

	/**
	 * Cache results of a function call, no clean-up.
	 */
	public static <I, O> Fun<I, O> fun(Fun<I, O> fun) {
		var isEnteredFun = ThreadLocal.withInitial(() -> false);
		var results = new ConcurrentHashMap<I, O>();
		return in -> {
			var isEnteredFun0 = isEnteredFun.get();
			if (!isEnteredFun0) {
				isEnteredFun.set(true);
				try {
					return results.computeIfAbsent(in, fun::apply);
				} finally {
					isEnteredFun.set(isEnteredFun0);
				}
			} else
				return fail("use funRec() instead");
		};
	}

	public static <I, O> Fun<I, O> funRec(Fun<I, O> fun) {
		var results = new ConcurrentHashMap<I, O>();
		return in -> {
			var out = results.get(in);
			if (out == null)
				results.put(in, out = fun.apply(in));
			return out;
		};
	}

	/**
	 * Memoizer for a parameterless function, guaranteeing a single call.
	 */
	public static <T> Source<T> future(Source<T> source) {
		return new Source<>() {
			private volatile T result;

			public T g() {
				if (result == null)
					synchronized (this) {
						if (result == null) {
							result = source.g();
							notifyAll();
						} else
							while (result == null)
								Wait.object(this);
					}
				return result;
			}
		};
	}

	/**
	 * Cache results of a function call, clean-up using clock algorithm as cache
	 * exceeded the given size.
	 */
	public static <I, O> Fun<I, O> limited(Fun<I, O> fun, int size) {
		return new Fun<>() {
			class R {
				private State state = State.EMPTY__;
				private I input;
				private O output;
			}

			private Map<I, R> map = new HashMap<>();
			private R[] array = new R[size];
			private int p = 0;

			{
				for (var i = 0; i < size; i++)
					array[i] = new R();
			}

			public synchronized O apply(I in) {
				var r = map.get(in);

				if (r == null) {
					while ((r = array[p]).state == State.FLAGGED) {
						r.state = State.INUSE_;
						p = (p + 1) % size;
					}

					if (r.state == State.INUSE_)
						map.remove(r.input);

					r.input = in;
					r.output = fun.apply(in);
					map.put(in, r);
				}

				r.state = State.FLAGGED;
				return r.output;
			}
		};
	}

	/**
	 * Cache results of a function call, removes the least recently used result as
	 * cache exceeded the given size.
	 */
	public static <I, O> Fun<I, O> queued(Fun<I, O> fun, int size) {
		return new Fun<>() {
			class R {
				int age;
				int index;
				I input;
				O output;
			}

			private int time;
			private Map<I, R> map = new HashMap<>();
			private PriorityQueue<R> queue = new PriorityQueue<>(R.class, size, (r0, r1) -> r0.age - r1.age);

			public synchronized O apply(I in) {
				var r = map.get(in);

				if (r == null) {
					if (size <= map.size())
						map.remove(queue.extractMin().input);

					r = new R();
					r.input = in;
					r.output = fun.apply(in);
					map.put(in, r);
				} else
					queue.remove(r.index);

				r.age = time++;
				r.index = queue.add(r);

				return r.output;
			}
		};
	}

	/**
	 * Thread-safe caching of function call results. Guarantee only one dispatch for
	 * an input parameter. No clean-up.
	 */
	public static <I, O> Fun<I, O> reentrant(Fun<I, O> fun) {
		var results = new ConcurrentHashMap<I, Source<O>>();
		return in -> results.computeIfAbsent(in, in_ -> future(() -> fun.apply(in_))).g();
	}

	/**
	 * Simplest memoizer for a parameterless function.
	 */
	public static <T> Source<T> source(Source<T> source) {
		return new Source<>() {
			private T result;

			public synchronized T g() {
				return result = result != null ? result : source.g();
			}
		};
	}

	/**
	 * Time-bounded memoizer for a parameterless function.
	 */
	public static <T> Source<T> timed(Source<T> source, long duration) {
		return new Source<>() {
			private long timestamp = 0;
			private T result;

			public synchronized T g() {
				var current = System.currentTimeMillis();
				if (result == null || timestamp + duration < current) {
					timestamp = current;
					result = source.g();
				}
				return result;
			}
		};
	}

}
