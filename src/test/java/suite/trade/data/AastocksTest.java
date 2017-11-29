package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AastocksTest {

	private Aastocks aa = new Aastocks();

	@Test
	public void testHsi() {
		float hsi = aa.hsi();
		System.out.println(hsi);
		assertTrue(20000f < hsi);
	}

	@Test
	public void testQuote() {
		float quote = aa.quote("0005.HK");
		System.out.println(quote);
		assertTrue(70f < quote);
	}

}
