package org.instructionexecutor.io;

import java.io.IOException;
import java.io.Reader;

import org.instructionexecutor.io.IndexedIo.IndexedInput;

public class IndexedReader implements IndexedInput {

	private Reader in;
	private StringBuilder sb = new StringBuilder();

	public IndexedReader(Reader in) {
		this.in = in;
	}

	@Override
	public synchronized int read(int p) {
		while (p >= sb.length()) {
			int c;

			try {
				c = in.read();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			if (c >= 0)
				sb.append((char) c);
			else
				break;
		}

		return p < sb.length() ? sb.charAt(p) : -1;
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
