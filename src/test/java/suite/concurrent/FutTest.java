package suite.concurrent;

import static org.junit.Assert.assertEquals;
import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import primal.Verbs.New;
import primal.Verbs.Sleep;
import primal.Verbs.Start;

public class FutTest {

	@Test
	public void test() {
		var value = 1;

		var fut = Fut.of(() -> {
			Sleep.quietly(1000l);
			System.out.println("Evaluated");
			return value;
		});

		var nc = new AtomicInteger();
		var count = 128;

		forInt(count).map(i -> New.thread(() -> {
			if (fut.get() == value)
				nc.incrementAndGet();
			else
				fail();
		})).collect(Start::thenJoin);

		assertEquals(count, nc.get());
	}

}
