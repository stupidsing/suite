package org.instructionexecutor.io;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 * Implements input and output devices that read/write at specified byte
 * positions rather than serially.
 * 
 * @author ywsing
 */
public class IndexedIo {

	public interface IndexedInput {
		public void fetch();

		public int read(int p);
	}

	public interface IndexedOutput {
		public void write(int p, char c);

		public void flush();
	}

	public static class IndexedInputStream implements IndexedInput {
		private Reader in;
		private StringBuilder inBuffer = new StringBuilder();

		public IndexedInputStream(Reader in) {
			this.in = in;
		}

		@Override
		public void fetch() {
		}

		@Override
		public int read(int p) {
			while (p >= inBuffer.length()) {
				int c;

				try {
					c = in.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

				if (c >= 0)
					inBuffer.append((char) c);
				else
					break;
			}

			return p < inBuffer.length() ? inBuffer.charAt(p) : -1;
		}

		public void setIn(Reader in) {
			this.in = in;
		}
	}

	public static class IndexedOutputStream implements IndexedOutput {
		private Writer out;
		private StringBuilder outBuffer = new StringBuilder();

		public IndexedOutputStream(Writer out) {
			this.out = out;
		}

		@Override
		public void write(int p, char c) {
			if (p >= outBuffer.length())
				outBuffer.setLength(p + 1);

			outBuffer.setCharAt(p, c);
		}

		@Override
		public void flush() {
			try {
				out.write(outBuffer.toString());
				out.flush();
			} catch (IOException ex) {
				throw new RuntimeException(ex);
			}

			outBuffer.setLength(0);
		}

		public void setOut(Writer out) {
			this.out = out;
		}
	}

}
