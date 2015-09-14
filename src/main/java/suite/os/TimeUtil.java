package suite.os;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import suite.util.FunUtil.Source;

public class TimeUtil {

	public class TimedResult<T> {
		public T result;
		public long duration;
		public int nGcs;
		public long gcDuration;

		public TimedResult(T result, long duration, int nGcs, long gcDuration) {
			this.result = result;
			this.duration = duration;
			this.nGcs = nGcs;
			this.gcDuration = gcDuration;
		}
	}

	public <T> TimedResult<T> time(Source<T> source) {
		List<GarbageCollectorMXBean> gcBeans0 = ManagementFactory.getGarbageCollectorMXBeans();
		long t0 = System.currentTimeMillis();
		int nGcs0 = gcBeans0.size();
		long gcDuration0 = gcBeans0.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

		T t = source.source();

		List<GarbageCollectorMXBean> gcBeans1 = ManagementFactory.getGarbageCollectorMXBeans();
		long t1 = System.currentTimeMillis();
		int nGcs1 = gcBeans1.size();
		long gcDuration1 = gcBeans1.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

		return new TimedResult<>(t, t1 - t0, nGcs1 - nGcs0, gcDuration1 - gcDuration0);
	}

}
