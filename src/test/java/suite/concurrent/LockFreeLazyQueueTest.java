package suite.concurrent;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class LockFreeLazyQueueTest {

	@Test
	public void test() {
		LockFreeLazyQueue<Integer> lfq = new LockFreeLazyQueue<>();
		for (int i = 0; i < 256; i++) {
			for (int j = 0; j < 256; j++)
				lfq.enqueue(j);
			for (int j = 0; j < 256; j++)
				assertEquals(j, (int) lfq.dequeue());
		}
	}

}
