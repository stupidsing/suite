package suite.util;

import static suite.util.Friends.rethrow;

import suite.os.Log_;
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
			Log_.error(ex);
		}
	}

	public void join_() {
		rethrow(() -> {
			join();
			return this;
		});
	}

}
