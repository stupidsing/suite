package suite.algo;

import java.util.HashMap;
import java.util.Map;

public class UnionFind<T> {

	private class Record {
		private T parent;
		private int rank;

		public Record(T parent) {
			this.parent = parent;
		}
	}

	private Map<T, Record> nodes = new HashMap<>();

	public void union(T t0, T t1) {
		Record pair0 = find_(t0);
		Record pair1 = find_(t1);

		if (pair0 != pair1)
			if (pair0.rank < pair1.rank)
				pair0.parent = t1;
			else if (pair1.rank < pair0.rank)
				pair1.parent = t0;
			else if (pair0.rank == pair1.rank) {
				pair1.parent = t0;
				pair0.rank++;
			}
	}

	public T find(T t) {
		return find_(t).parent;
	}

	private Record find_(T t) {
		Record record = getRecord(t);
		if (!t.equals(record.parent)) {
			Record parentRecord = find_(record.parent);
			record.parent = parentRecord.parent;
			return parentRecord;
		} else
			return record;
	}

	private Record getRecord(T t) {
		return nodes.computeIfAbsent(t, Record::new);
	}

}
