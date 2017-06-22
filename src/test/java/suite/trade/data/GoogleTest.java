package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import suite.util.To;

public class GoogleTest {

	private Google google = new Google();

	@Test
	public void test() {
		Map<String, Float> priceBySymbol = google.quote(To.set("0002.HK", "0005.HK"));
		assertTrue(0f < priceBySymbol.get("0002.HK"));
		assertTrue(0f < priceBySymbol.get("0005.HK"));
	}

}
