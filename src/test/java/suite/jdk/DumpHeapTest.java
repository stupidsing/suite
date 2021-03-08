package suite.jdk;

import static primal.statics.Rethrow.ex;

import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.Test;

import com.sun.management.HotSpotDiagnosticMXBean;

public class DumpHeapTest {

	@Test
	public void test() {
		var live = true; // dump only the live objects
		dumpHeap("heap.hprof", live);
	}

	/**
	 * @param filename name of the heap dump file
	 * @param live     whether to dump only the live objects
	 */
	private String dumpHeap(String filename, boolean live) {
		return ex(() -> {
			var bean = "com.sun.management:type=HotSpotDiagnostic";
			var mbs = ManagementFactory.getPlatformMBeanServer();
			var hsdmb = ManagementFactory.newPlatformMXBeanProxy(mbs, bean, HotSpotDiagnosticMXBean.class);
			hsdmb.dumpHeap(filename, live);
			return filename;
		});
	}

}
