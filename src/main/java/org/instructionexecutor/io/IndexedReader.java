package org.instructionexecutor.io;

import java.io.IOException;
import java.io.Reader;

import org.instructionexecutor.io.IndexedIo.IndexedInput;

public class IndexedReader implements IndexedInput {

	private static final int bufferLimit = 256;

	private Reader in;
	private int offset;
	private StringBuilder sb = new StringBuilder();

	public IndexedReader(Reader in) {
		this.in = in;
	}

	@Override
	public synchronized int read(int p) {
		while (p - offset >= sb.length()) {
			int c;

			try {
				c = in.read();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			if (c >= 0) {
				sb.append((char) c);

				if (sb.length() > bufferLimit) {
					int shift = sb.length() - bufferLimit / 2;
					sb.delete(0, shift);
					offset += shift;
				}
			} else
				break;
		}

		int index = p - offset;

		if (index >= 0)
			return index < sb.length() ? sb.charAt(index) : -1;
		else
			throw new RuntimeException("Cannot unwind flushed input buffer");
	}

	@Override
	public synchronized void close() {
		try {
			in.close();
		} catch (IOException ex) {
			throw new RuntimeException(ex);
		}
	}

}
