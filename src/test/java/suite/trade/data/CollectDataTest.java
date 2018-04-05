package suite.trade.data;

import java.io.FileOutputStream;
import java.io.IOException;

import org.junit.Test;

import suite.http.HttpUtil;
import suite.streamlet.Streamlet;
import suite.trade.Forex;
import suite.trade.TimeRange;
import suite.util.Copy;
import suite.util.Thread_;
import suite.util.To;

public class CollectDataTest {

	private Forex forex = new Forex();
	private Hkex hkex = new Hkex();
	private Yahoo yahoo = new Yahoo();

	@Test
	public void test() throws IOException {
		Streamlet<String> equities = Streamlet.concat( //
				hkex.queryCompanies().map(company -> company.symbol), //
				forex.invertedCurrencies.keys());

		for (var code : equities) {
			String urlString = yahoo.tableUrl(code, TimeRange.ages());
			var url = To.url(urlString);

			try (var fos = new FileOutputStream("/data/storey/markets/" + code + ".csv")) {
				Copy.stream(To.inputStream(HttpUtil.get(url).out), fos);
			}

			Thread_.sleepQuietly(2000);
		}
	}

}
