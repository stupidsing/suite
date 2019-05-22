package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class HsbcTest {

	private Hsbc hsbc = new Hsbc();

	@Test
	public void test() {
		var symbol = "0019.HK";
		var quote = hsbc.quote(Set.of(symbol)).get(symbol);
		System.out.println(symbol + " = " + quote);
		assertTrue(80f < quote);
	}

}
