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

	public class Pointer implements IPointer<T> {
		private int position;

		private Pointer(int position) {
			this.position = position;
		}

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

		public Pointer tail() {
			return new Pointer(position + 1);
		}
	}

	public IndexedSourceReader(Source<T> source) {
		this.source = source;
	}

	public IPointer<T> pointer() {
		return new Pointer(0);
	}

}
