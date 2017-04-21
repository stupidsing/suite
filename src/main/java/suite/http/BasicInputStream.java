package suite.http;

import java.io.IOException;
import java.io.InputStream;

/**
 * Implements an input stream using a given input stream. Extends this class to
 * provide additional functionality.
 *
 * @author ywsing
 */
public abstract class BasicInputStream extends InputStream {

	private InputStream is;

	public BasicInputStream(InputStream is) {
		this.is = is;
	}

	public int available() throws IOException {
		return is.available();
	}

	public void close() throws IOException {
		is.close();
	}

	public void mark(int readLimit) {
		throw new UnsupportedOperationException();
	}

	public boolean markSupported() {
		return false;
	}

	public int read() throws IOException {
		return is.read();
	}

	public int read(byte[] bytes) throws IOException {
		return is.read(bytes);
	}

	public int read(byte[] bytes, int offset, int length) throws IOException {
		return is.read(bytes, offset, length);
	}

	public void reset() {
		throw new UnsupportedOperationException();
	}

	public long skip(long n) {
		throw new UnsupportedOperationException();
	}

}
