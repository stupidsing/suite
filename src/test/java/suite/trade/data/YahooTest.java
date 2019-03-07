package suite.trade.data;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import suite.inspect.Dump;
import suite.os.Log_;
import suite.streamlet.FunUtil2.Fun2;
import suite.trade.Time;
import suite.trade.TimeRange;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testL1() {
		test(yahoo::dataSourceL1);
	}

	@Test
	public void testL1Adjust() {
		var time0 = Time.ofEpochSec(1490578200l);
		var timex = Time.ofEpochSec(1497490200l);
		Dump.details(yahoo.dataSourceL1("0012.HK", TimeRange.of(time0, timex)));
	}

	// @Test // takes too long
	public void testL1All() {
		var hkexFactBook = new HkexFactBook();
		var symbols = hkexFactBook.queryMainBoardCompanies(2016);
		// hkexFactBook.queryLeadingCompaniesByMarketCap(2016);
		for (var symbol : symbols)
			try {
				yahoo.dataSourceL1(symbol, TimeRange.daysBefore(31));
			} catch (Exception ex) {
				Log_.error(ex);
			}
	}

	// @Test
	public void testYql() {
		test(yahoo::dataSourceYql);
	}

	private void test(Fun2<String, TimeRange, DataSource> fun) {
		var symbol = "0005.HK";

		var ds = fun.apply(symbol, TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1))).validate();
		System.out.println(ds.recent(symbol, 9));

		var tsLength = ds.ts.length;
		var pricesLength = ds.prices.length;
		assertTrue(tsLength == pricesLength);
		assertTrue(0 < tsLength);
	}

}
