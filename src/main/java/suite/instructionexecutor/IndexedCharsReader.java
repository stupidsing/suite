package suite.instructionexecutor;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import suite.primitive.Chars;
import suite.util.Util;

public class IndexedCharsReader {

	private static int bufferSize = 4096;
	private static int maxBuffers = 32;

	private Reader in;
	private int offset = 0;
	private List<Chars> queue = new ArrayList<>();

	public class Pointer {
		private int position;

		private Pointer(int position) {
			this.position = position;
		}

		public Chars head() {
			return read(position);
		}

		public Pointer tail() {
			return new Pointer(position + 1);
		}
	}

	public IndexedCharsReader(Reader in) {
		this.in = in;
	}

	public Pointer pointer() {
		return new Pointer(0);
	}

	public synchronized Chars read(int p) {
		while (p - offset >= queue.size()) {
			Chars chars;

			if (in != null)
				try {
					char buffer[] = new char[bufferSize];
					int nCharsRead = in.read(buffer);
					chars = nCharsRead >= 0 ? Chars.of(buffer, 0, nCharsRead) : null;
				} catch (IOException ex) {
					throw new RuntimeException(ex);
				}
			else
				chars = null;

			if (chars != null) {
				int size1 = queue.size() + 1;

				if (size1 > maxBuffers) {
					int shift = size1 - maxBuffers / 2;
					queue = new ArrayList<>(Util.right(queue, shift));
					offset += shift;
				}

				queue.add(chars);
			} else {
				Util.closeQuietly(in);
				in = null;
				break;
			}
		}

		int index = p - offset;

		if (index >= 0)
			return index < queue.size() ? queue.get(index) : null;
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
