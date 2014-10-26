package suite.instructionexecutor;

import suite.immutable.IPointer;

public class IndexedReader<T> {

	private Read<T> read;
	private int size;

	public interface Read<T> {
		public T read(int position);
	}

	public class Pointer implements IPointer<T> {
		private int position;

		private Pointer(int position) {
			this.position = position;
		}

		public T head() {
			return read.read(position);
		}

		public Pointer tail() {
			int position1 = position + 1;
			return position1 < size ? new Pointer(position1) : null;
		}
	}

	public IndexedReader(Read<T> read, int size) {
		this.read = read;
		this.size = size;
	}

	public IPointer<T> pointer() {
		return new Pointer(0);
	}

}
