package suite.exchange;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ExchangeTest {

	@Test
	public void test() {
		var exchange = new Exchange();
		var a = exchange.newParticipant();
		var b = exchange.newParticipant();
		a.order("S").new_(+100, 1f);
		b.order("S").new_(-100, 1f);
		assertEquals(100d, a.summary().investedAmount, 0d);
		assertEquals(100d, b.summary().investedAmount, 0d);
	}

}
