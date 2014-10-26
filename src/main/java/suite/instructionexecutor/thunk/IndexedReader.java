package suite.instructionexecutor.thunk;

import java.util.function.IntFunction;

import suite.immutable.IPointer;

public class IndexedReader<T> {

	private IntFunction<T> read;
	private int size;

	public IndexedReader(IntFunction<T> read, int size) {
		this.read = read;
		this.size = size;
	}

	public IPointer<T> pointer() {
		return pointer(0);
	}

	private IPointer<T> pointer(int position) {
		return new IPointer<T>() {
			public T head() {
				return read.apply(position);
			}

			@Override
			public IPointer<T> tail() {
				int position1 = position + 1;
				return position1 < size ? pointer(position1) : null;
			}
		};
	}

}
