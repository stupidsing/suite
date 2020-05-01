package suite.jdk;

import org.junit.jupiter.api.Test;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;

public class ThreadMxBeanTest {

	@Test
	public void test() {
		var threadBean = ManagementFactory.getThreadMXBean();
		System.out.println(threadBean.getThreadCount());

		var threadInfos = threadBean.dumpAllThreads(false, false);

		for (var threadInfo : threadInfos)
			if (threadInfo.getThreadState() == State.RUNNABLE)
				System.out.println(threadInfo.toString());
	}

}
