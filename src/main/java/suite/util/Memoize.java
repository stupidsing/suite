package suite.util;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Memoize {

	private enum State {
		EMPTY__, INUSE_, FLAGGED
	}

	public static <I, O> Fun<I, O> fun(Fun<I, O> fun) {
		Map<I, O> results = new ConcurrentHashMap<>();
		return in -> results.computeIfAbsent(in, in_ -> fun.apply(in_));
	}

	public static <I, O> Fun<I, O> funLimited(Class<O> clazz, Fun<I, O> fun, int size) {
		return new Fun<I, O>() {
			class R {
				State state = State.EMPTY__;
				I input;
				O output;
			}

			private Map<I, R> map = new HashMap<>();
			private R array[] = new R[size];
			private int p = 0;

			{
				for (int i = 0; i < size; i++)
					array[i] = new R();
			}

			public synchronized O apply(I in) {
				R r = map.get(in);

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

	public static <I, O> Fun<I, O> funReentrant(Fun<I, O> fun) {
		Map<I, Source<O>> results = new ConcurrentHashMap<>();
		return in -> results.computeIfAbsent(in, in_ -> () -> fun.apply(in_)).source();
	}

	public static <T> Source<T> future(Source<T> source) {
		return new Source<T>() {
			private volatile T result;

			public T source() {
				if (result == null)
					synchronized (this) {
						if (result == null) {
							result = source.source();
							notifyAll();
						} else
							while (result == null)
								Util.wait(this);
					}
				return result;
			}
		};
	}

	public static <T> Source<T> memoize(Source<T> source) {
		return new Source<T>() {
			private T result;

			public synchronized T source() {
				return result = result != null ? result : source.source();
			}
		};
	}

	public static <T> Source<T> timed(Source<T> source, long duration) {
		return new Source<T>() {
			private long timestamp = 0;
			private T result;

			public synchronized T source() {
				long current = System.currentTimeMillis();
				if (result == null || timestamp + duration < current) {
					timestamp = current;
					result = source.source();
				}
				return result;
			}
		};
	}

}
