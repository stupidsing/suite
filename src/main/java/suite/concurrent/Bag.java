package suite.concurrent;

import java.util.ArrayDeque;
import java.util.Iterator;

import primal.Ob;
import suite.persistent.PerList;

public class Bag<S> implements Iterable<S> {

	private CasReference<PerList<S>> cr = new CasReference<>(PerList.end());

	public void add(S s) {
		cr.apply(list0 -> PerList.cons(s, list0));
	}

	public void remove(S s) {
		cr.apply(list0 -> {
			var queue = new ArrayDeque<S>();
			for (var s_ : list0)
				if (!Ob.equals(s, s_))
					queue.addLast(s_);
			var list1 = PerList.<S> end();
			S s_;
			while ((s_ = queue.pop()) != null)
				list1 = PerList.cons(s_, list1);
			return list1;
		});
	}

	public Iterator<S> iterator() {
		return cr.getReference().iterator();
	}

}
