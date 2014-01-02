package suite.algo;

import java.util.Map;

import suite.util.DefaultFunMap;
import suite.util.FunUtil.Fun;
import suite.util.Pair;

public class UnionFind<T> {

	private Map<T, Pair<T, Integer>> parents = new DefaultFunMap<>(new Fun<T, Pair<T, Integer>>() {
		public Pair<T, Integer> apply(T t) {
			return Pair.create(t, 0);
		}
	});

	public void union(T t0, T t1) {
		Pair<T, Integer> pair0 = find0(t0);
		Pair<T, Integer> pair1 = find0(t1);

		if (pair0.t1 < pair1.t1)
			pair0.t0 = t1;
		else if (pair0.t1 > pair1.t1)
			pair1.t0 = t0;
		else if (pair0.t1 == pair1.t1) {
			pair1.t0 = t0;
			pair0.t1++;
		}
	}

	public T find(T t) {
		return find0(t).t0;
	}

	private Pair<T, Integer> find0(T t) {
		return find0(parents.get(t));
	}

	private Pair<T, Integer> find0(Pair<T, Integer> pair) {
		Pair<T, Integer> parent = parents.get(pair.t0);
		if (parent.t0 != pair.t0) {
			parent.t0 = find0(parent.t0).t0;
			return parent;
		} else
			return pair;
	}

}
