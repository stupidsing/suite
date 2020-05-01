package suite.instructionexecutor.thunk;

import suite.persistent.PerPointer;

import java.util.function.IntFunction;

public class IndexedReader<T> {

	private IntFunction<T> read;
	private int size;

	public static <T> PerPointer<T> of(IntFunction<T> read, int size) {
		return new IndexedReader<>(read, size).pointer(0);
	}

	private IndexedReader(IntFunction<T> read, int size) {
		this.read = read;
		this.size = size;
	}

	private PerPointer<T> pointer(int position) {
		return new PerPointer<>() {
			public T head() {
				return position < size ? read.apply(position) : null;
			}

			@Override
			public PerPointer<T> tail() {
				return pointer(position + 1);
			}
		};
	}

}
