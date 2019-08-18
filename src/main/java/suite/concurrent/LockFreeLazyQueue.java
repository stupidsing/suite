package suite.concurrent;

import primal.adt.Mutable;
import primal.adt.Pair;

public class LockFreeLazyQueue<T> {

	private BackFront empty = new BackFront(0, null, null, null);
	private CasReference<BackFront> cas = new CasReference<>(empty);

	private class BackFront {
		private int size;
		private BackFront back;
		private BackFront front;
		private T t;

		private BackFront(int size, BackFront back, BackFront front, T t) {
			this.size = size;
			this.back = back;
			this.front = front;
			this.t = t;
		}
	}

	public LockFreeLazyQueue() {
		empty.back = empty;
		empty.front = empty;
	}

	public void enqueue(T t) {
		cas.apply(backFront -> enqueue_(t, backFront));
	}

	/**
	 * @return null if the queue is empty.
	 */
	public T dequeue() {
		var result = Mutable.<T> nil();
		cas.apply(backFront -> {
			var pair = dequeue_(backFront);
			result.set(pair.k);
			return pair.v;
		});
		return result.value();
	}

	private BackFront enqueue_(T t, BackFront bf0) {
		BackFront bf1;
		if (bf0.size != 0)
			bf1 = make(enqueue_(t, bf0.back), bf0.front, bf0.t);
		else
			bf1 = new BackFront(1, empty, empty, t);
		return bf1;
	}

	private Pair<T, BackFront> dequeue_(BackFront bf0) {
		T t;
		BackFront bf1;
		if (1 < bf0.size) {
			var pair = dequeue_(bf0.front);
			t = bf0.t;
			bf1 = make(bf0.back, pair.v, pair.k);
		} else {
			t = bf0.t;
			bf1 = empty;
		}
		return Pair.of(t, bf1);
	}

	private BackFront make(BackFront back, BackFront front, T t) {
		var size = back.size + front.size + 1;
		if (back.size <= front.size)
			return new BackFront(size, back, front, t);
		else {
			var pair = dequeue_(back);
			return new BackFront(size, pair.v, enqueue_(pair.k, front), t);
		}
	}

}
