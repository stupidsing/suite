package suite.adt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;

import org.junit.jupiter.api.Test;

import suite.util.To;

public class PriorityQueueTest {

	private Random random = new Random();

	@Test
	public void test3000() {
		for (int i = 0; i < 3000; i++)
			test();
	}

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
