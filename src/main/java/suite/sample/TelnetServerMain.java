package suite.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;

import suite.os.LogUtil;
import suite.os.SocketUtil;
import suite.util.Copy;
import suite.util.RunUtil;
import suite.util.RunUtil.ExecutableProgram;
import suite.util.Thread_;

public class TelnetServerMain extends ExecutableProgram {

	public static void main(String[] args) {
		RunUtil.run(TelnetServerMain.class, args);
	}

	@Override
	protected boolean run(String[] args) throws IOException {
		new SocketUtil().listenIo(2323, (sis, sos) -> new Server().serve(sis, sos));
		return true;
	}

	private class Server {
		private Set<Thread> threads = new HashSet<>();

		private abstract class InterruptibleThread extends Thread {
			protected abstract void run_() throws Exception;

			public void run() {
				try {
					run_();
				} catch (InterruptedException | InterruptedIOException ex) {
				} catch (Exception ex) {
					LogUtil.error(ex);
				} finally {

					// if we are not being interrupted by another thread, issue
					// interrupt signal to other threads
					if (!isInterrupted())
						for (var thread : threads)
							if (thread != this)
								thread.interrupt();
				}
			}
		}

		private class CopyThread extends InterruptibleThread {
			private InputStream is;
			private OutputStream os;

			private CopyThread(InputStream is, OutputStream os) {
				this.is = is;
				this.os = os;
			}

			protected void run_() throws IOException {
				try (InputStream is_ = is; OutputStream os_ = os) {
					Copy.stream(is_, os_);
				}
			}
		}

		private void serve(InputStream sis, OutputStream sos) throws IOException {

			// kills the process if client closes the stream;
			// closes the stream if process is terminated/ended output.
			// therefore we need the interruption mechanism.
			Process process = Runtime.getRuntime().exec("bash");
			InputStream pis = process.getInputStream();
			InputStream pes = process.getErrorStream();
			OutputStream pos = process.getOutputStream();

			try {
				threads.add(new CopyThread(pis, sos));
				threads.add(new CopyThread(pes, sos));
				threads.add(new CopyThread(sis, pos));
				threads.add(new InterruptibleThread() {
					protected void run_() throws InterruptedException {
						process.waitFor();
					}
				});

				Thread_.startJoin(threads);
			} finally {
				process.destroy();
			}
		}
	}

}
