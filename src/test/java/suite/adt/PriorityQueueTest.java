package suite.adt;

import org.junit.jupiter.api.Test;
import suite.util.To;

import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PriorityQueueTest {

	private Random random = new Random();

	@Test
	public void test() {
		var floats = To.vector(1024, i -> random.nextDouble());

		var pq = new PriorityQueue<>(Float.class, 1024, Float::compare);

		for (var f : floats)
			pq.add(f);

		var actual = new HashSet<Float>();
		var expect = new HashSet<Float>();

		for (var f : floats)
			actual.add(f);

		var f0 = Float.MIN_VALUE;

		while (!pq.isEmpty()) {
			var f = pq.extractMin();
			assertTrue(f0 <= f);
			expect.add(f0 = f);
		}

		assertEquals(actual, expect);
	}

}
