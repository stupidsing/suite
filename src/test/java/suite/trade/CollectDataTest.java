package suite.trade;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;

import org.junit.Test;

import suite.http.HttpUtil;
import suite.streamlet.Streamlet;
import suite.util.Copy;
import suite.util.Rethrow;
import suite.util.To;
import suite.util.Util;

public class CollectDataTest {

	@Test
	public void test() throws IOException {
		LocalDate frDate = LocalDate.of(1900, 1, 1);
		LocalDate toDate = LocalDate.of(2020, 1, 1);

		Streamlet<String> equities = Streamlet.concat( //
				new Hkex().companies.map(company -> company.code + ".HK"), //
				new Forex().invertedCurrencies.map((ccy, name) -> ccy));

		for (String code : equities) {
			String urlString = DataSource.yahooUrl(code, frDate, toDate);

			System.out.println(urlString);
			URL url = Rethrow.ex(() -> new URL(urlString));

			try (FileOutputStream fos = new FileOutputStream("/data/storey/markets/" + code + ".csv")) {
				Copy.stream(To.inputStream(HttpUtil.http("GET", url).out), fos);
			}

			Util.sleepQuietly(2000);
		}
	}

}
