package suite.util;

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

	@Override
	public int available() throws IOException {
		return is.available();
	}

	@Override
	public void close() throws IOException {
		is.close();
	}

	@Override
	public void mark(int readLimit) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean markSupported() {
		return false;
	}

	@Override
	public int read() throws IOException {
		return is.read();
	}

	@Override
	public int read(byte[] bytes) throws IOException {
		return is.read(bytes);
	}

	@Override
	public int read(byte[] bs, int offset, int length) throws IOException {
		return is.read(bs, offset, length);
	}

	@Override
	public void reset() {
		throw new UnsupportedOperationException();
	}

	@Override
	public long skip(long n) {
		throw new UnsupportedOperationException();
	}

}
