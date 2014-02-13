package suite.instructionexecutor;

public class IndexedReaderPointer {

	private int position;
	private IndexedReader reader;

	public IndexedReaderPointer(IndexedReader reader) {
		this(0, reader);
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
