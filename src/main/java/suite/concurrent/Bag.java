package suite.concurrent;

import java.util.Iterator;

import suite.immutable.IList;

public class Bag<S> implements Iterable<S> {

	private CasReference<IList<S>> cr = new CasReference<>(IList.end());

	public void add(S s) {
		cr.apply(list0 -> IList.cons(s, list0));
	}

	public Iterator<S> iterator() {
		return cr.getReference().iterator();
	}

}
