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

	private double threshold = 0.15;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005";
	private String market = "HK";
	private LocalDate frDate = LocalDate.of(2012, 2, 26);
	private LocalDate toDate = LocalDate.of(2017, 2, 26);

	@Test
	public void backTest() {
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
		int nTransactions = 0;
		double totalNetGain = 0;
		int signals[] = new int[prices.length];

		for (int d = 0; d < prices.length; d++) {

			// buy if ratio is positive; sell if ratio is negative
			// sell nFutureDays after
			double price = prices[d];
			int signal = getSignal(prices, d);
			int signal0 = nFutureDays < d ? signals[d - nFutureDays] : 0;
			int buySell = signal - signal0;

			signals[d] = signal;
			nLots += buySell;
			nTransactions += Math.abs(buySell);
			totalNetGain += -buySell * price;

			if (signal != 0)
				System.out.println("d = " + d //
						+ ", price = " + price //
						+ ", signal = " + signal //
						+ ", nLots = " + nLots);
		}

		System.out.println("number of transactions = " + nTransactions);
		System.out.println("total net gain = " + totalNetGain);
	}

	// 1 = buy, 0 = no change, -1 = sell
	private int getSignal(double prices[], int d) {
		if (nPastDays < d && d + nFutureDays < prices.length) {
			double price0 = prices[d];
			double predict = predictEightDaysAfter(prices, d);
			double ratio = (predict - price0) / price0;
			int signal;

			if (ratio < -threshold)
				signal = -1;
			else if (threshold < ratio)
				signal = 1;
			else
				signal = 0;
			return signal;
		} else
			return 0;
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
