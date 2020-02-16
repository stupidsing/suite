package suite.jdk;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;

import org.junit.jupiter.api.Test;

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
