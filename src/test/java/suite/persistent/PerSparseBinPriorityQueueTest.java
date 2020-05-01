package suite.persistent;

import org.junit.jupiter.api.Test;
import primal.Verbs.Compare;

import java.util.ArrayList;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PerSparseBinPriorityQueueTest {

	@Test
	public void test() {
		var size = 4096;

		var list = new ArrayList<Integer>();
		for (var i = 0; i < size; i++)
			list.add(i);
		Collections.shuffle(list);

		var pq = new PerSparseBinPriorityQueue<Integer>(Compare::objects);

		for (var i : list)
			pq = pq.add(i);

		for (var i = 0; i < size; i++) {
			assertEquals(i, (Object) pq.findMin());
			pq = pq.deleteMin();
		}
	}

}
