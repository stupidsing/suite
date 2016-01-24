package suite.adt;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LockFreeQueueTest {

	@Test
	public void test() {
		LockFreeQueue<Integer> lfq = new LockFreeQueue<>();
		for (int i = 0; i < 256; i++)
			lfq.enqueue(i);
		for (int i = 0; i < 256; i++)
			assertEquals(i, (int) lfq.dequeue());
	}

}
