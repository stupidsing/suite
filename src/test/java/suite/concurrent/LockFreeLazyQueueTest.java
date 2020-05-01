package suite.concurrent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LockFreeLazyQueueTest {

	@Test
	public void test() {
		var lfq = new LockFreeLazyQueue<>();
		for (var i = 0; i < 256; i++) {
			for (var j = 0; j < 256; j++)
				lfq.enqueue(j);
			for (var j = 0; j < 256; j++)
				assertEquals(j, (int) lfq.dequeue());
		}
	}

}
