package suite.trade.data;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;

import org.junit.Test;

import suite.http.HttpUtil;
import suite.streamlet.Streamlet;
import suite.trade.DatePeriod;
import suite.trade.Forex;
import suite.util.Copy;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.Util;

public class CollectDataTest {

	@Test
	public void test() throws IOException {
		Streamlet<String> equities = Streamlet.concat( //
				new Hkex().queryCompanies().map(company -> company.code), //
				new Forex().invertedCurrencies.map((ccy, name) -> ccy));

		for (String code : equities) {
			String urlString = new Yahoo().tableUrl(code, DatePeriod.ages());

			System.out.println(urlString);
			URL url = Rethrow.ex(() -> new URL(urlString));

			try (FileOutputStream fos = new FileOutputStream("/data/storey/markets/" + code + ".csv")) {
				Copy.stream(To.inputStream(HttpUtil.http("GET", url).out), fos);
			}

			Util.sleepQuietly(2000);
		}
	}

}
