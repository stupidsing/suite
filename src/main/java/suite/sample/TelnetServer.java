package suite.sample;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import suite.net.CopyStreamThread;
import suite.util.SocketUtil;
import suite.util.SocketUtil.Io;

public class TelnetServer {

	public static void main(String args[]) throws IOException {
		new TelnetServer().run();
	}

	private void run() throws IOException {
		SocketUtil.listen(2323, new Io() {
			public void serve(InputStream sis, OutputStream sos) throws IOException {

				// Kills the process if client closes the stream;
				// closes the stream if process is terminated/ended output.
				// Therefore we need a quitter object and some pollings.
				AtomicBoolean quitter = new AtomicBoolean(false);
				Process process = Runtime.getRuntime().exec("bash");

				try (InputStream pis = process.getInputStream();
						InputStream pes = process.getErrorStream();
						OutputStream pos = process.getOutputStream()) {
					Thread threads[] = { new CopyStreamThread(pis, sos, quitter) //
							, new CopyStreamThread(pes, sos, quitter) //
							, new CopyStreamThread(sis, pos, quitter) };

					for (Thread thread : threads)
						thread.start();

					while (!quitter.get())
						try {
							process.exitValue();
							break;
						} catch (IllegalThreadStateException ex) {
							Thread.sleep(100);
						}
				} catch (InterruptedException ex) {
					throw new RuntimeException(ex);
				} finally {
					quitter.set(true);
					process.destroy();
				}
			}
		});
	}

}
