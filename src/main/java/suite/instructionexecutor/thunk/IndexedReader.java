package suite.instructionexecutor.thunk;

import java.util.function.IntFunction;

import suite.immutable.IPointer;

public class IndexedReader<T> {

	private IntFunction<T> read;
	private int size;

	public static <T> IPointer<T> of(IntFunction<T> read, int size) {
		return new IndexedReader<>(read, size).pointer(0);
	}

	private IndexedReader(IntFunction<T> read, int size) {
		this.read = read;
		this.size = size;
	}

	private IPointer<T> pointer(int position) {
		return new IPointer<>() {
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
