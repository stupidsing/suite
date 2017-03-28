package suite.trade;

import java.net.URL;
import java.time.LocalDate;

import org.junit.Test;

import suite.Constants;
import suite.http.HttpUtil;
import suite.os.StoreCache;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Outlet;
import suite.util.Rethrow;

public class TradeTest {

	@Test
	public void backTest() {
		double threshold = 0.15;
		int nPastDays = 64;
		int nFutureDays = 8;
		String stockCode = "0005";
		String market = "HK";
		LocalDate frDate = LocalDate.of(2012, 2, 26);
		LocalDate toDate = LocalDate.of(2017, 2, 26);

		String url = "http://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode + "." + market //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";

		Bytes keyBytes = Bytes.of(url.getBytes(Constants.charset));
		StoreCache sc = new StoreCache();
		Outlet<Bytes> outlet = sc.getOutlet(keyBytes, () -> HttpUtil.http("GET", Rethrow.ex(() -> new URL(url))).out);

		double prices[] = outlet //
				.collect(BytesUtil.split(Bytes.of((byte) 10))) //
				.skip(1) //
				.map(bytes -> new String(bytes.toBytes(), Constants.charset).split(",")) //
				.sort((a0, a1) -> a0[0].compareTo(a1[0])) //
				.collect(As.arrayOfDoubles(array -> Double.parseDouble(array[1])));

		// a.collect(As.sequenced((index, array) -> index + "," +
		// String.join(",", array))) .collect(As.joined("\n"));

		// Date, Open, High, Low, Close, Volume, Adj Close

		int nLots = 0;
		double totalNetGain = 0;
		int signals[] = new int[prices.length];

		for (int d = nPastDays; d + nFutureDays < prices.length; d++) {
			double price0 = prices[d];
			double actual = prices[d + nFutureDays];
			double estimated = predictEightDaysAfter(prices, d);
			double ratio = (estimated - price0) / price0;

			// buy if ratio is positive; sell if ratio is negative
			// sell nFutureDays after
			if (ratio < -threshold || threshold < ratio) {
				int signal = signals[d] = ratio < 0 ? -1 : 1;
				nLots += signal;
				nLots -= signals[d - nFutureDays];
				double netGain = (actual - price0) * Math.signum(ratio);
				System.out.println("d = " + d //
						+ ", price = " + price0 //
						+ ", signal = " + (signal < 0 ? "SELL" : "BUY") //
						+ ", nLots = " + nLots //
						+ ", ratio = " + ratio //
						+ ", net gain = " + netGain);
				totalNetGain += netGain;
			}
		}

		System.out.println("total net gain = " + totalNetGain);
	}

	// input: prices between (d - 64) and d days
	// output: estimated price on (d + 8) day
	private double predictEightDaysAfter(double prices[], int d) {
		double sum = 0;
		for (int i = d - 64; i < d; i++)
			sum += prices[i];
		return sum / 64;
	}

}
