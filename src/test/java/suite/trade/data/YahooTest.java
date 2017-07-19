package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.inspect.Dump;
import suite.os.LogUtil;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.FunUtil2.Fun2;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testL1() {
		test(yahoo::dataSourceL1);
	}

	@Test
	public void testL1Adjust() {
		Time time0 = Time.ofEpochSec(1490578200l);
		Time timex = Time.ofEpochSec(1497490200l);
		Dump.out(yahoo.dataSourceL1("0012.HK", TimeRange.of(time0, timex)));
	}

	// @Test
	public void testL1All() {
		HkexFactBook hkexFactBook = new HkexFactBook();
		Iterable<String> symbols = hkexFactBook.queryMainBoardCompanies(2016);
		// hkexFactBook.queryLeadingCompaniesByMarketCap(2016);
		for (String symbol : symbols) {
			try {
				yahoo.dataSourceL1(symbol, TimeRange.daysBefore(31));
			} catch (Exception ex) {
				LogUtil.error(ex);
			}
		}
	}

	// @Test
	public void testYql() {
		test(yahoo::dataSourceYql);
	}

	private void test(Fun2<String, TimeRange, DataSource> fun) {
		String symbol = "0005.HK";

		DataSource ds = fun.apply(symbol, TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1)));
		ds.validate();
		System.out.println(ds.recent(symbol, 9));

		int tsLength = ds.ts.length;
		int pricesLength = ds.prices.length;
		assertTrue(tsLength == pricesLength);
		assertTrue(0 < tsLength);
	}

}
