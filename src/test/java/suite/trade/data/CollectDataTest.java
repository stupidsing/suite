package suite.trade.data;

import org.junit.Test;

import suite.http.HttpUtil;
import suite.streamlet.Streamlet;
import suite.trade.Forex;
import suite.trade.TimeRange;
import suite.util.Thread_;
import suite.util.To;

public class CollectDataTest {

	private Forex forex = new Forex();
	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void test() {
		var equities = Streamlet.concat( //
				hkex.queryCompanies().map(company -> company.symbol), //
				forex.invertedCurrencies.keys());

		for (var code : equities) {
			var url = yahoo.tableUrl(code, TimeRange.ages());
			HttpUtil.get(url).out().collect(To.file("/data/storey/markets/" + code + ".csv"));
			Thread_.sleepQuietly(2000);
		}
	}

}
