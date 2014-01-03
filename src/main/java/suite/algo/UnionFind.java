package suite.algo;

import java.util.Map;

import suite.util.DefaultFunMap;
import suite.util.FunUtil.Fun;

public class UnionFind<T> {

	private class Record {
		private T parent;
		private int rank;

		public Record(T parent, int rank) {
			this.parent = parent;
			this.rank = rank;
		}
	}

	private Map<T, Record> nodes = new DefaultFunMap<>(new Fun<T, Record>() {
		public Record apply(T t) {
			return new Record(t, 0);
		}
	});

	public void union(T t0, T t1) {
		Record pair0 = find0(t0);
		Record pair1 = find0(t1);

		if (pair0.rank < pair1.rank)
			pair0.parent = t1;
		else if (pair0.rank > pair1.rank)
			pair1.parent = t0;
		else if (pair0.rank == pair1.rank) {
			pair1.parent = t0;
			pair0.rank++;
		}
	}

	public T find(T t) {
		return find0(t).parent;
	}

	private Record find0(T t) {
		return find0(nodes.get(t));
	}

	private Record find0(Record pair) {
		Record parent = nodes.get(pair.parent);
		if (parent.parent != pair.parent) {
			parent.parent = find0(parent.parent).parent;
			return parent;
		} else
			return pair;
	}

}
