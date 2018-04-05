package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.util.Object_;

public class IBinPriorityQueueTest {

	@Test
	public void test() {
		var size = 4096;

		List<Integer> list = new ArrayList<>();
		for (var i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		IBinPriorityQueue<Integer> pq = new IBinPriorityQueue<>(Object_::compare);

		for (int i : list)
			pq = pq.add(i);

		for (var i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
