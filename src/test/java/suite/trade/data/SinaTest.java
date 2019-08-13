package suite.trade.data;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import primal.MoreVerbs.Read;
import suite.inspect.Dump;

public class SinaTest {

	private Sina sina = new Sina();

	@Test
	public void testQueryFactor() {
		var factor = sina.queryFactor("0005.HK");
		assertNotNull(factor);
		Dump.details(factor);
	}

	@Test
	public void testQuote() {
		var quotes = sina.quote(Read.each("0003.HK", "0005.HK").toSet());
		assertNotNull(0f < quotes.get("0003.HK"));
		assertNotNull(0f < quotes.get("0005.HK"));
		Dump.details(quotes);
	}

}
