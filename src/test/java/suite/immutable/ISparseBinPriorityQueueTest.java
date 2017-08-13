package suite.immutable;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import suite.util.Object_;

public class ISparseBinPriorityQueueTest {

	@Test
	public void test() {
		int size = 4096;

		List<Integer> list = new ArrayList<>();
		for (int i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		ISparseBinPriorityQueue<Integer> pq = new ISparseBinPriorityQueue<>(Object_::compare);

		for (int i : list)
			pq = pq.add(i);

		for (int i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
