package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.IntInt_Int;
import suite.util.Util;

public class RangeMinQueryTest {

	@Test
	public void test() {
		assertEquals(6, rangeMinQuery(new Integer[] { 0, 5, 2, 5, 4, 3, 1, 6, 3, }).apply(3, 8));
	}

	private <T extends Comparable<T>> IntInt_Int rangeMinQuery(T[] ts) {
		class Rmq {
			private int mid;
			private int minIndex;
			private Rmq l, r;

			private Rmq(int mid, int minIndex, Rmq l, Rmq r) {
				this.mid = mid;
				this.minIndex = minIndex;
				this.l = l;
				this.r = r;
			}
		}

		var root = new Object() {
			private Rmq build(int s, int e) {
				if (s == e)
					return null;
				else if (s + 1 == e)
					return new Rmq(s, s, null, null);
				else {
					var mid = Util.mid(s, e);
					var l = build(s, mid);
					var r = build(mid, e);
					var isLeft = ts[l.minIndex].compareTo(ts[r.minIndex]);
					return new Rmq(mid, (isLeft < 0 ? l : r).minIndex, l, r);
				}
			}
		}.build(0, ts.length);

		return (s, e) -> new Object() {
			private int query(int s, int e, Rmq rmq) {
				if (rmq != null) {
					var min = rmq.mid;
					int mi;
					if (s < rmq.mid && ts[mi = query(s, rmq.mid, rmq.l)].compareTo(ts[min]) < 0)
						min = mi;
					if (rmq.mid < e && ts[mi = query(rmq.mid, e, rmq.r)].compareTo(ts[min]) < 0)
						min = mi;
					return min;
				} else
					return s;
			}
		}.query(s, e, root);
	}

}
