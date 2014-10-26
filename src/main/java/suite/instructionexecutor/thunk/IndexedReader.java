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
				return position < size ? read.apply(position) : null;
			}

			@Override
			public IPointer<T> tail() {
				return pointer(position + 1);
			}
		};
	}

}
