package suite.instructionexecutor;

import java.io.Reader;

public class IndexedReaderPointer {

	private int position;
	private IndexedReader reader;

	public IndexedReaderPointer(Reader reader) {
		this(0, new IndexedReader(reader));
	}

	private IndexedReaderPointer(int position, IndexedReader reader) {
		this.position = position;
		this.reader = reader;
	}

	public int head() {
		return reader.read(position);
	}

	public IndexedReaderPointer tail() {
		return new IndexedReaderPointer(position + 1, reader);
	}

}
