package suite.instructionexecutor.thunk;

import java.util.ArrayList;
import java.util.List;

import suite.immutable.IPointer;
import suite.streamlet.Streamlet;
import suite.util.Util;

public class IndexedSourceReader<T> {

	private static int maxBuffers = 32;

	private Streamlet<T> streamlet;
	private int offset = 0;
	private List<T> queue = new ArrayList<>();

	public static <T> IPointer<T> of(Streamlet<T> st) {
		return new IndexedSourceReader<>(st).pointer(0);
	}

	private IndexedSourceReader(Streamlet<T> streamlet) {
		this.streamlet = streamlet;
	}

	private IPointer<T> pointer(int position) {
		return new IPointer<T>() {
			public T head() {
				synchronized (IndexedSourceReader.this) {
					while (position - offset >= queue.size()) {
						T t = streamlet != null ? streamlet.next() : null;

						if (t != null) {
							int size1 = queue.size() + 1;

							if (size1 > maxBuffers) {
								int shift = size1 - maxBuffers / 2;
								queue = new ArrayList<>(Util.right(queue, shift));
								offset += shift;
							}

							queue.add(t);
						} else {
							streamlet = null;
							break;
						}
					}

					int index = position - offset;

					if (index >= 0)
						return index < queue.size() ? queue.get(index) : null;
					else
						throw new RuntimeException("Cannot unwind flushed input buffer");
				}
			}

			public IPointer<T> tail() {
				return pointer(position + 1);
			}
		};
	}

}
