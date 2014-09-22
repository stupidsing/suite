package suite.fp;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.stream.LongStream;

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

		LongStream gcDuration = ManagementFactory.getGarbageCollectorMXBeans().stream()
				.mapToLong(GarbageCollectorMXBean::getCollectionTime);
		System.out.println("GC took " + gcDuration.sum() + "ms");
		System.out.println("Program took " + (end - start) / 1000000l + "ms");
	}

}
