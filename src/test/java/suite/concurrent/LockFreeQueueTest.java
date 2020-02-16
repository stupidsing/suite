package suite.concurrent;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class LockFreeQueueTest {

	@Test
	public void test() {
		var lfq = new LockFreeQueue<>();
		for (var i = 0; i < 256; i++) {
			for (var j = 0; j < 256; j++)
				lfq.enqueue(j);
			for (var j = 0; j < 256; j++)
				assertEquals(j, (int) lfq.dequeue());
		}
	}

}
