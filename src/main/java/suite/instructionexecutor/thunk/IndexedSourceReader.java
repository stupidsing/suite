package suite.instructionexecutor.thunk;

import java.util.ArrayList;
import java.util.List;

import suite.immutable.IPointer;
import suite.util.FunUtil.Source;
import suite.util.Util;

public class IndexedSourceReader<T> {

	private static int maxBuffers = 32;

	private Source<T> source;
	private int offset = 0;
	private List<T> queue = new ArrayList<>();

	public IndexedSourceReader(Source<T> source) {
		this.source = source;
	}

	public IPointer<T> pointer() {
		return pointer(0);
	}

	private IPointer<T> pointer(int position) {
		return new IPointer<T>() {
			public T head() {
				synchronized (IndexedSourceReader.this) {
					while (position - offset >= queue.size()) {
						T t = source != null ? source.source() : null;

						if (t != null) {
							int size1 = queue.size() + 1;

							if (size1 > maxBuffers) {
								int shift = size1 - maxBuffers / 2;
								queue = new ArrayList<>(Util.right(queue, shift));
								offset += shift;
							}

							queue.add(t);
						} else {
							source = null;
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
