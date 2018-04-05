package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import suite.util.Object_;

public class ISkewedBinPriorityQueueTest {

	@Test
	public void test() {
		var size = 4096;

		var list = new ArrayList<Integer>();
		for (var i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		ISkewedBinPriorityQueue<Integer> pq = new ISkewedBinPriorityQueue<>(Object_::compare);

		for (var i : list)
			pq = pq.add(i);

		for (var i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
