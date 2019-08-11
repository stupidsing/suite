package suite.trade.data;

import org.junit.Test;

import primal.Verbs.Sleep;
import primal.streamlet.Streamlet;
import suite.http.HttpUtil;
import suite.trade.Forex;
import suite.trade.TimeRange;
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
			Sleep.quietly(2000);
		}
	}

}
