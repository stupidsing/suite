package suite.trade.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

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
				forex.invertedCurrencies.map((ccy, name) -> ccy));

		for (String code : equities) {
			String urlString = yahoo.tableUrl(code, TimeRange.ages());
			URL url = To.url(urlString);

			try (FileOutputStream fos = new FileOutputStream("/data/storey/markets/" + code + ".csv")) {
				Copy.stream(To.inputStream(HttpUtil.get(url).out), fos);
			}

			Thread_.sleepQuietly(2000);
		}
	}

}
