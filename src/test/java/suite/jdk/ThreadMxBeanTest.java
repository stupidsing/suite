package suite.jdk;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;

import org.junit.Test;

public class ThreadMxBeanTest {

	@Test
	public void test() {
		ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
		System.out.println(threadBean.getThreadCount());

		ThreadInfo[] threadInfos = threadBean.dumpAllThreads(false, false);

		for (ThreadInfo threadInfo : threadInfos)
			if (threadInfo.getThreadState() == State.RUNNABLE)
				System.out.println(threadInfo.toString());
	}

}
