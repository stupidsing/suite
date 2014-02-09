package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.util.Util;

public class SparseBinomialPriorityQueueTest {

	@Test
	public void test() {
		List<Integer> list = new ArrayList<Integer>();
		for (int i = 0; i < 4096; i++)
			list.add(i);
		Collections.shuffle(list);

		SparseBinomialPriorityQueue<Integer> pq = new SparseBinomialPriorityQueue<>(Util.<Integer> comparator());

		for (int i : list)
			pq = pq.add(i);

		for (int i = 0; i < 4096; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
