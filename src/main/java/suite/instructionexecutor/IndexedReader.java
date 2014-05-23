package suite.instructionexecutor;

import java.io.IOException;
import java.io.Reader;

import suite.util.Util;

public class IndexedReader {

	private static int bufferLimit = 1024;

	private Reader in;
	private int offset = 0;
	private char buffer[] = new char[bufferLimit / 2];
	private StringBuilder sb = new StringBuilder();

	public IndexedReader(Reader in) {
		this.in = in;
	}

	public synchronized int read(int p) {
		while (p - offset >= sb.length()) {
			int nCharsRead;

			if (in != null)
				try {
					nCharsRead = in.read(buffer);
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			else
				nCharsRead = -1;

			if (nCharsRead >= 0) {
				int size1 = sb.length() + nCharsRead;

				if (size1 > bufferLimit) {
					int shift = size1 - bufferLimit / 2;
					sb.delete(0, shift);
					offset += shift;
				}

				sb.append(buffer, 0, nCharsRead);
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
