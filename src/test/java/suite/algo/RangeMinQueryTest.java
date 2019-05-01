package suite.algo;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import suite.primitive.IntInt_Int;

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

			private Rmq(int mid, int minIndex) {
				this.mid = mid;
				this.minIndex = minIndex;
			}
		}

		var root = new Object() {
			private Rmq build(int s, int e) {
				if (s == e)
					return null;
				else if (s + 1 == e)
					return new Rmq(s, s);
				else {
					var mid = (int) (s + (long) e) / 2;
					Rmq l = build(s, mid);
					Rmq r = build(mid, e);
					var isLeft = ts[l.minIndex].compareTo(ts[r.minIndex]);
					var rmq = new Rmq(mid, (isLeft < 0 ? l : r).minIndex);
					rmq.l = l;
					rmq.r = r;
					return rmq;
				}
			}
		}.build(0, ts.length);

		return (s, e) -> new Object() {
			private int query(int s, int e, Rmq rmq) {
				var min = rmq.mid;
				int mi;
				if (rmq.l != null && s < rmq.mid && ts[mi = query(s, rmq.mid, rmq.l)].compareTo(ts[min]) < 0)
					min = mi;
				if (rmq.r != null && rmq.mid < e && ts[mi = query(rmq.mid, e, rmq.r)].compareTo(ts[min]) < 0)
					min = mi;
				return min;
			}
		}.query(s, e, root);
	}

}
