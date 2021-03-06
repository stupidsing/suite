package suite.exchange;

import org.junit.jupiter.api.Test;

import suite.math.Math_;

public class ExchangeTest {

	@Test
	public void test() {
		var exchange = new Exchange();
		var a = exchange.newParticipant();
		var b = exchange.newParticipant();
		a.order("S").new_(+100, 1f);
		b.order("S").new_(-100, 1f);
		Math_.verifyEquals(100d, a.summary().investedAmount);
		Math_.verifyEquals(100d, b.summary().investedAmount);
	}

}
