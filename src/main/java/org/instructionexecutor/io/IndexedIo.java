package org.instructionexecutor.io;

import java.io.Closeable;
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

	public interface IndexedInput extends Closeable {
		public int read(int p);
	}

	public interface IndexedOutput extends Closeable {
		public void write(int p, char c);
	}

	public static class IndexedInputStream implements IndexedInput {
		private Reader in;
		private StringBuilder inBuffer = new StringBuilder();

		public IndexedInputStream(Reader in) {
			this.in = in;
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

		@Override
		public void close() throws IOException {
			in.close();
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
		public void close() throws IOException {
			out.write(outBuffer.toString());
			outBuffer.setLength(0);
			out.close();
		}

		public void setOut(Writer out) {
			this.out = out;
		}
	}

}
