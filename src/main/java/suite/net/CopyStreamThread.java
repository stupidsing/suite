package suite.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import suite.util.LogUtil;

public class CopyStreamThread extends Thread {

	private InputStream is;
	private OutputStream os;
	private AtomicBoolean quitter;

	public CopyStreamThread(InputStream is, OutputStream os, AtomicBoolean quitter) {
		this.is = is;
		this.os = os;
		this.quitter = quitter;
	}

	public void run() {
		try {
			byte buffer[] = new byte[4096];

			while (!quitter.get()) {
				int avail = is.available();

				if (avail > 0) {
					int nBytesRead = is.read(buffer);

					if (nBytesRead > 0) {
						os.write(buffer, 0, nBytesRead);
						os.flush();
					} else
						break;
				} else if (avail == 0)
					Thread.sleep(100);
				else
					break;
			}
		} catch (Exception ex) {
			LogUtil.error(ex);
		} finally {
			quitter.set(true);
		}
	}

}
