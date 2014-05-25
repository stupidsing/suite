package suite.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class Memoize {

	public static <I, O> Fun<I, O> byInput(Fun<I, O> fun) {
		Map<Object, Object> results = new ConcurrentHashMap<>();

		return in -> {
			O result;
			if (!results.containsKey(in))
				results.put(in, result = fun.apply(in));
			else {
				@SuppressWarnings("unchecked")
				O o = (O) results.get(in);
				result = o;
			}
			return result;
		};
	}

	public static <T> Source<T> timed(Source<T> source) {
		return timed(source, 30 * 1000l);
	}

	public static <T> Source<T> timed(Source<T> source, long duration) {
		return new Source<T>() {
			private long timestamp = 0;
			private T result;

			public synchronized T source() {
				long current = System.currentTimeMillis();
				if (result == null || current > timestamp + duration) {
					timestamp = current;
					result = source.source();
				}
				return result;
			}
		};
	}

}
