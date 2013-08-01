package suite.util;

import suite.util.FunUtil.Source;

public class MemoizerUtil {

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
