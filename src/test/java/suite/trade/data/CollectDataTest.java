package suite.trade.data;

import org.junit.Test;

import suite.http.HttpUtil;
import suite.os.FileUtil;
import suite.streamlet.Streamlet;
import suite.trade.Forex;
import suite.trade.TimeRange;
import suite.util.Copy;
import suite.util.Thread_;

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

			HttpUtil.get(url).inputStream().doRead(is -> {
				FileUtil.out("/data/storey/markets/" + code + ".csv").doWrite(os -> Copy.stream(is, os));
				return is;
			});

			Thread_.sleepQuietly(2000);
		}
	}

}
