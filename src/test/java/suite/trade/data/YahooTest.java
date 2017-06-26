package suite.trade.data;

import static org.junit.Assert.assertTrue;

import java.util.function.BiFunction;

import org.junit.Test;

import suite.inspect.Dump;
import suite.os.LogUtil;
import suite.trade.Time;
import suite.trade.TimeRange;
import suite.util.Thread_;

public class YahooTest {

	private Yahoo yahoo = new Yahoo();

	@Test
	public void testL1() {
		test(yahoo::dataSourceL1);
	}

	@Test
	public void testL1Adjust() {
		Time frDate = Time.ofEpochUtcSecond(1490578200l);
		Time toDate = Time.ofEpochUtcSecond(1497490200l);
		Dump.out(yahoo.dataSourceL1("0012.HK", TimeRange.of(frDate, toDate)));
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
			Thread_.sleepQuietly(5000l);
		}
	}

	@Test
	public void testYql() {
		test(yahoo::dataSourceYql);
	}

	private void test(BiFunction<String, TimeRange, DataSource> fun) {
		DataSource ds = fun.apply("0005.HK", TimeRange.of(Time.of(2016, 1, 1), Time.of(2017, 1, 1)));

		ds.validate();

		int datesLength = ds.dates.length;
		int pricesLength = ds.prices.length;
		assertTrue(datesLength == pricesLength);
		assertTrue(0 < datesLength);
	}

}
