package suite.trade.data;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.junit.Test;

import suite.inspect.Dump;
import suite.streamlet.Read;

public class SinaTest {

	private Sina sina = new Sina();

	@Test
	public void testQueryFactor() {
		var factor = sina.queryFactor("0005.HK");
		assertNotNull(factor);
		Dump.out(factor);
	}

	@Test
	public void testQuote() {
		Map<String, Float> quotes = sina.quote(Read.each("0003.HK", "0005.HK").toSet());
		assertNotNull(0f < quotes.get("0003.HK"));
		assertNotNull(0f < quotes.get("0005.HK"));
		Dump.out(quotes);
	}

}
