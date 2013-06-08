package org.instructionexecutor.io;

import java.io.IOException;
import java.io.Reader;

/**
 * Implements input and output devices that read/write at specified byte
 * positions rather than serially.
 * 
 * @author ywsing
 */
public class IndexedIo {

	public interface IndexedInput {
		public int read(int p);

		public void close();
	}

	public static class IndexedReader implements IndexedInput {
		private Reader in;
		private StringBuilder sb = new StringBuilder();

		public IndexedReader(Reader in) {
			this.in = in;
		}

		@Override
		public int read(int p) {
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

		public void close() {
			try {
				in.close();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}
		}
	}

}
