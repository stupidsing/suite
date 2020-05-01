package suite.concurrent;

import org.junit.jupiter.api.Test;
import primal.Verbs.New;
import primal.Verbs.Sleep;
import primal.Verbs.Start;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static primal.statics.Fail.fail;
import static suite.util.Streamlet_.forInt;

public class RunOnceTest {

	@Test
	public void test() {
		var value = 1;

		var fut = RunOnce.of(() -> {
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
