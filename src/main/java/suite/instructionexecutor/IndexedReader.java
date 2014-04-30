package suite.instructionexecutor;

import java.io.IOException;
import java.io.Reader;

import suite.util.Util;

public class IndexedReader {

	private static int bufferLimit = 256;

	private Reader in;
	private int offset = 0;
	private StringBuilder sb = new StringBuilder();

	public IndexedReader(Reader in) {
		this.in = in;
	}

	public synchronized int read(int p) {
		while (p - offset >= sb.length()) {
			int c;

			if (in != null)
				try {
					c = in.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			else
				c = -1;

			if (c >= 0) {
				sb.append((char) c);

				if (sb.length() > bufferLimit) {
					int shift = sb.length() - bufferLimit / 2;
					sb.delete(0, shift);
					offset += shift;
				}
			} else {
				Util.closeQuietly(in);
				in = null;
				break;
			}
		}

		int index = p - offset;

		if (index >= 0)
			return index < sb.length() ? sb.charAt(index) : -1;
		else
			throw new RuntimeException("Cannot unwind flushed input buffer");
	}

	public synchronized void close() {
		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
