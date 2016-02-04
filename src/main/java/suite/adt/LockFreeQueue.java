package suite.adt;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import suite.concurrent.CasReference;
import suite.immutable.IList;

public class LockFreeQueue<T> {

	private CasReference<BackFront> cas = new CasReference<>(new BackFront(IList.end(), IList.end()));

	private class BackFront {
		private IList<T> back;
		private IList<T> front;

		private BackFront(IList<T> back, IList<T> front) {
			this.back = back;
			this.front = front;
		}
	}

	public void enqueue(T t) {
		cas.apply(queue0 -> new BackFront(IList.cons(t, queue0.back), queue0.front));
	}

	/**
	 * @return null if the queue is empty.
	 */
	public T dequeue() {
		List<T> result = new ArrayList<>(Arrays.asList((T) null));

		cas.apply(fb0 -> {
			IList<T> back = fb0.back;
			IList<T> front = fb0.front;
			if (front.isEmpty()) { // Reverse elements from back to front
				for (T t_ : fb0.back)
					front = IList.cons(t_, front);
				back = IList.end();
			}
			if (!front.isEmpty()) {
				result.set(0, front.head);
				return new BackFront(back, front.tail);
			} else {
				result.set(0, null);
				return fb0;
			}
		});

		return result.get(0);
	}

}
