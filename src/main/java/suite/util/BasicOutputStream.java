package suite.util;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implements an output stream using a given output stream. Extends this class
 * to provide additional functionality.
 *
 * @author ywsing
 */
public abstract class BasicOutputStream extends OutputStream {

	private OutputStream os;

	public BasicOutputStream(OutputStream os) {
		this.os = os;
	}

	@Override
	public void close() throws IOException {
		os.close();
	}

	@Override
	public void flush() throws IOException {
		os.flush();
	}

	@Override
	public void write(byte[] bs, int offset, int length) throws IOException {
		os.write(bs, offset, length);
	}

	@Override
	public void write(byte[] bs) throws IOException {
		os.write(bs);
	}

	@Override
	public void write(int b) throws IOException {
		os.write(b);
	}

}
