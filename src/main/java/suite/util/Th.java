package suite.util;

import suite.os.LogUtil;
import suite.util.Thread_.RunnableEx;

public class Th extends Thread {

	private RunnableEx runnable;

	public Th(RunnableEx runnable) {
		this.runnable = runnable;
	}

	public void run() {
		try {
			runnable.run();
		} catch (Exception ex) {
			LogUtil.error(ex);
		}
	}

	public void join_() {
		try {
			join();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		}
	}

}
