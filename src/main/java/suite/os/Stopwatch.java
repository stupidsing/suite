package suite.os;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;

import primal.fp.Funs.Source;

public class Stopwatch<T> {

	public T result;
	public long duration;
	public long nGcs;
	public long gcDuration;

	public Stopwatch(T result, long duration, long nGcs, long gcDuration) {
		this.result = result;
		this.duration = duration;
		this.nGcs = nGcs;
		this.gcDuration = gcDuration;
	}

	public static <T> Stopwatch<T> of(Source<T> source) {
		var gcBeans0 = ManagementFactory.getGarbageCollectorMXBeans();
		var t0 = System.nanoTime();
		var nGcs0 = gcBeans0.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
		var gcDuration0 = gcBeans0.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

		var t = source.g();

		var gcBeans1 = ManagementFactory.getGarbageCollectorMXBeans();
		var t1 = System.nanoTime();
		var nGcs1 = gcBeans1.stream().mapToLong(GarbageCollectorMXBean::getCollectionCount).sum();
		var gcDuration1 = gcBeans1.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

		return new Stopwatch<>(t, (t1 - t0) / 1000000, nGcs1 - nGcs0, gcDuration1 - gcDuration0);
	}

}
