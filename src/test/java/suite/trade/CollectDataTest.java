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
			String urlString = "http://chart.finance.yahoo.com/table.csv" //
					+ "?s=" + code //
					+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
					+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
					+ "&g=d&ignore=.csv";

			System.out.println(urlString);

			URL url = Rethrow.ex(() -> new URL(urlString));
			Copy.stream(To.inputStream(HttpUtil.http("GET", url).out),
					new FileOutputStream("/data/storey/stocks/" + code + ".csv"));

			Util.sleepQuietly(2000);
		}
	}

}
