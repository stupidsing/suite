package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;

import org.junit.Test;

import suite.util.Object_;

public class IBinPriorityQueueTest {

	@Test
	public void test() {
		var size = 4096;

		var list = new ArrayList<Integer>();
		for (var i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		IBinPriorityQueue<Integer> pq = new IBinPriorityQueue<>(Object_::compare);

		for (var i : list)
			pq = pq.add(i);

		for (var i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
