package suite.util;

import suite.util.FunUtil.Source;

public class MemoizerUtil {

	public static class TimedMemoizer<T> {
		private long duration;
		private long lastUpdate = 0;
		private Source<T> source;
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

			if (result == null || current > lastUpdate + duration) {
				lastUpdate = current;
				result = source.source();
			}

			return result;
		}
	}

}
