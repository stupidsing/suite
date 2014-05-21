package suite.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import suite.util.FunUtil.Fun;
import suite.util.FunUtil.Source;

public class MemoizeUtil {

	public static <I, O> Fun<I, O> memoize(Fun<I, O> fun) {
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

	public static class TimedMemoizer<T> {
		private Source<T> source;
		private long duration;
		private long timestamp = 0;
		private T result;

		public TimedMemoizer(Source<T> source) {
			this(source, 300 * 1000l);
		}

		public TimedMemoizer(Source<T> source, long duration) {
			this.duration = duration;
			this.source = source;
		}

		public synchronized T get() {
			long current = System.currentTimeMillis();

			if (result == null || current > timestamp + duration) {
				timestamp = current;
				result = source.source();
			}

			return result;
		}
	}

}
