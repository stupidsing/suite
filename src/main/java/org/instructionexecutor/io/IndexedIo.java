package org.instructionexecutor.io;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import org.net.Bytes.BytesBuilder;

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
		public void write(int p, int c);
	}

	public static class IndexedInputStream implements IndexedInput {
		private InputStream in;
		private BytesBuilder inBuffer = new BytesBuilder();

		public IndexedInputStream(InputStream in) {
			this.in = in;
		}

		@Override
		public int read(int p) {
			while (p >= inBuffer.getSize()) {
				int c;

				try {
					c = in.read();
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}

				if (c >= 0)
					inBuffer.append((byte) c);
				else
					break;
			}

			return p < inBuffer.getSize() ? inBuffer.byteAt(p) : -1;
		}

		@Override
		public void close() throws IOException {
			in.close();
		}

		public void setIn(InputStream in) {
			this.in = in;
		}
	}

	public static class IndexedOutputStream implements IndexedOutput {
		private OutputStream out;
		private BytesBuilder outBuffer = new BytesBuilder();

		public IndexedOutputStream(OutputStream out) {
			this.out = out;
		}

		@Override
		public void write(int p, int c) {
			if (p >= outBuffer.getSize())
				outBuffer.extend(p + 1);

			outBuffer.setByteAt(p, (byte) c);
		}

		@Override
		public void close() throws IOException {
			out.write(outBuffer.toBytes().getBytes());
			outBuffer.clear();
			out.close();
		}

		public void setOut(PrintStream out) {
			this.out = out;
		}
	}

}
