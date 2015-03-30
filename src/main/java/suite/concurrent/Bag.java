package suite.concurrent;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicReference;

import suite.immutable.IList;

public class Bag<S> implements Iterable<S> {

	private AtomicReference<IList<S>> ar = new AtomicReference<>(IList.end());

	public void add(S s) {
		while (true) {
			IList<S> list0 = ar.get();
			IList<S> list1 = IList.cons(s, list0);
			if (!ar.compareAndSet(list0, list1))
				Thread.yield(); // back-off
			else
				break;
		}
	}

	public Iterator<S> iterator() {
		return ar.get().iterator();
	}

}
