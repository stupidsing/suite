package suite.concurrent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LockFreeQueueTest {

	@Test
	public void test() {
		LockFreeQueue<Integer> lfq = new LockFreeQueue<>();
		for (var i = 0; i < 256; i++) {
			for (var j = 0; j < 256; j++)
				lfq.enqueue(j);
			for (var j = 0; j < 256; j++)
				assertEquals(j, (int) lfq.dequeue());
		}
	}

}
