package suite.concurrent;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicStampedReference;

import suite.immutable.IList;

public class Bag<S> implements Iterable<S> {

	private AtomicStampedReference<IList<S>> ar = new AtomicStampedReference<>(IList.end(), 0);

	public void add(S s) {
		while (true) {
			int a[] = new int[1];
			IList<S> list0 = ar.get(a);
			IList<S> list1 = IList.cons(s, list0);
			int stamp = a[0];
			if (!ar.compareAndSet(list0, list1, stamp, stamp + 1))
				Thread.yield(); // Back-off
			else
				break;
		}
	}

	public Iterator<S> iterator() {
		return ar.getReference().iterator();
	}

}
