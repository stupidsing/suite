package suite.fp;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;

import org.junit.Test;

import suite.Suite;
import suite.lp.doer.Configuration.ProverConfig;
import suite.sample.Profiler;

public class PrecompileTest {

	@Test
	public void test() {
		long start = System.nanoTime();

		System.out.println(new Profiler().profile(() -> {
			ProverConfig pc = new ProverConfig();
			Suite.precompile("STANDARD", pc);
		}));

		long end = System.nanoTime();

		List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
		int nGcs = gcBeans.size();
		long gcDuration = gcBeans.stream().mapToLong(GarbageCollectorMXBean::getCollectionTime).sum();

		System.out.println(nGcs + " GC took " + gcDuration + "ms");
		System.out.println("Program took " + (end - start) / 1000000l + "ms");
	}

}
