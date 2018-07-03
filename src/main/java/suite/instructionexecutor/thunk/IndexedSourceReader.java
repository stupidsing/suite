package suite.instructionexecutor.thunk;

import java.util.ArrayList;
import java.util.List;

import suite.immutable.IPointer;
import suite.streamlet.FunUtil.Source;
import suite.util.Fail;
import suite.util.List_;

public class IndexedSourceReader<T> {

	private static int maxBuffers = 32;

	private Source<T> source;
	private int offset = 0;
	private List<T> queue = new ArrayList<>();

	public static <T> IPointer<T> of(Source<T> st) {
		return new IndexedSourceReader<>(st).pointer(0);
	}

	private IndexedSourceReader(Source<T> Source) {
		this.source = Source;
	}

	private IPointer<T> pointer(int position) {
		return new IPointer<>() {
			public T head() {
				synchronized (IndexedSourceReader.this) {
					while (queue.size() <= position - offset) {
						var t = source != null ? source.source() : null;

						if (t != null) {
							var size1 = queue.size() + 1;

							if (maxBuffers < size1) {
								var shift = size1 - maxBuffers / 2;
								queue = new ArrayList<>(List_.right(queue, shift));
								offset += shift;
							}

							queue.add(t);
						} else {
							source = null;
							break;
						}
					}

					var index = position - offset;

					if (0 <= index)
						return index < queue.size() ? queue.get(index) : null;
					else
						return Fail.t("cannot unwind flushed input buffer");
				}
			}

			public IPointer<T> tail() {
				return pointer(position + 1);
			}
		};
	}

}
