package suite.concurrent;

import java.util.ArrayDeque;
import java.util.Iterator;

import jersey.repackaged.com.google.common.base.Objects;
import suite.immutable.IList;

public class Bag<S> implements Iterable<S> {

	private CasReference<IList<S>> cr = new CasReference<>(IList.end());

	public void add(S s) {
		cr.apply(list0 -> IList.cons(s, list0));
	}

	public void remove(S s) {
		cr.apply(list0 -> {
			var queue = new ArrayDeque<S>();
			for (S s_ : list0)
				if (!Objects.equal(s, s_))
					queue.addLast(s_);
			IList<S> list1 = IList.end();
			S s_;
			while ((s_ = queue.pop()) != null)
				list1 = IList.cons(s_, list1);
			return list1;
		});
	}

	public Iterator<S> iterator() {
		return cr.getReference().iterator();
	}

}
