package suite.trade.data;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
