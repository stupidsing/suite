package suite.trade.data;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AastocksTest {

	private Aastocks aa = new Aastocks();

	@Test
	public void testHsi() {
		var hsi = aa.hsi();
		assertTrue(20000f < hsi);
	}

}
