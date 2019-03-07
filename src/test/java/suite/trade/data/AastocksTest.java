package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class AastocksTest {

	private Aastocks aa = new Aastocks();

	@Test
	public void testHsi() {
		var hsi = aa.hsi();
		assertTrue(20000f < hsi);
	}

}
