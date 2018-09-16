package suite.concurrent;

import static org.junit.Assert.assertEquals;
import static suite.util.Friends.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import suite.primitive.Ints_;
import suite.util.Thread_;

public class FutTest {

	@Test
	public void test() {
		var value = 1;

		var fut = Fut.of(() -> {
			Thread_.sleepQuietly(1000l);
			System.out.println("Evaluated");
			return value;
		});

		var nc = new AtomicInteger();
		var count = 128;

		Thread_.startJoin(Ints_.range(count).map(i -> Thread_.newThread(() -> {
			if (fut.get() == value)
				nc.incrementAndGet();
			else
				fail();
		})));

		assertEquals(count, nc.get());
	}

}
