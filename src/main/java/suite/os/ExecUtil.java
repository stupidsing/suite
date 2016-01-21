package suite.os;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import suite.util.Copy;

public class ExecUtil {

	public final int code;
	public final String out;
	public final String err;
	private Thread threads[];

	public ExecUtil(String command[], String in) throws IOException {
		InputStream bis = new ByteArrayInputStream(in.getBytes(FileUtil.charset));
		ByteArrayOutputStream bos0 = new ByteArrayOutputStream();
		ByteArrayOutputStream bos1 = new ByteArrayOutputStream();

		Process process = Runtime.getRuntime().exec(command);

		try {
			InputStream pis = process.getInputStream();
			InputStream pes = process.getErrorStream();
			OutputStream pos = process.getOutputStream();

			threads = new Thread[] { //
					Copy.streamByThread(pis, bos0), //
					Copy.streamByThread(pes, bos1), //
					Copy.streamByThread(bis, pos), };

			for (Thread thread : threads)
				thread.start();

			code = process.waitFor();

			for (Thread thread : threads)
				thread.join();
		} catch (InterruptedException ex) {
			throw new RuntimeException(ex);
		} finally {
			process.destroy();
		}

		out = new String(bos0.toByteArray(), FileUtil.charset);
		err = new String(bos1.toByteArray(), FileUtil.charset);
	}

	@Override
	public String toString() {
		return "code = " + code //
				+ "\nout = " + out //
				+ "\nerr = " + err;
	}

}
