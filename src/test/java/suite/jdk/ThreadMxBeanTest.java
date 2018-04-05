package suite.jdk;

import java.lang.Thread.State;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;

import org.junit.Test;

public class ThreadMxBeanTest {

	@Test
	public void test() {
		var threadBean = ManagementFactory.getThreadMXBean();
		System.out.println(threadBean.getThreadCount());

		ThreadInfo[] threadInfos = threadBean.dumpAllThreads(false, false);

		for (var threadInfo : threadInfos)
			if (threadInfo.getThreadState() == State.RUNNABLE)
				System.out.println(threadInfo.toString());
	}

}
