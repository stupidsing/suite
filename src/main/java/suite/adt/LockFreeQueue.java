package suite.adt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.concurrent.CasReference;
import suite.immutable.IList;

public class LockFreeQueue<T> {

	private CasReference<FrontBack> cas = new CasReference<>(new FrontBack(IList.end(), IList.end()));

	private class FrontBack {
		private IList<T> front;
		private IList<T> back;

		private FrontBack(IList<T> front, IList<T> back) {
			this.front = front;
			this.back = back;
		}
	}

	public void enqueue(T t) {
		cas.apply(queue0 -> new FrontBack(IList.cons(t, queue0.front), queue0.back));
	}

	/**
	 * @return null if the queue is empty.
	 */
	public T dequeue() {
		List<T> result = new ArrayList<>(Arrays.asList((T) null));

		cas.apply(fb0 -> {
			IList<T> front = fb0.front;
			IList<T> back = fb0.back;
			if (back.isEmpty()) { // Reverse elements from front to back
				for (T t_ : fb0.front)
					back = IList.cons(t_, back);
				front = IList.end();
			}
			if (!back.isEmpty()) {
				result.set(0, back.head);
				return new FrontBack(front, back.tail);
			} else {
				result.set(0, null);
				return fb0;
			}
		});

		return result.get(0);
	}

}
