package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

public class GoogleTest {

	private Google google = new Google();

	@Test
	public void test() {
		var priceBySymbol = google.quote(Set.of("0002.HK", "0005.HK"));
		assertTrue(0f < priceBySymbol.get("0002.HK"));
		assertTrue(0f < priceBySymbol.get("0005.HK"));
	}

}
