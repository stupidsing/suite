package suite.util;

import suite.util.FunUtil.Source;

public class TimeUtil {

	public class TimedResult<T> {
		public long duration;
		public T result;

		public TimedResult(long duration, T result) {
			this.duration = duration;
			this.result = result;
		}
	}

	public <T> TimedResult<T> time(Source<T> source) {
		long start = System.currentTimeMillis();
		T t = source.source();
		return new TimedResult<>(System.currentTimeMillis() - start, t);
	}

}
