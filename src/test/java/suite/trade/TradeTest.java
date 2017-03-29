package suite.trade;

import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import suite.Constants;
import suite.http.HttpUtil;
import suite.os.StoreCache;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.streamlet.Read;
import suite.util.Rethrow;

public class TradeTest {

	private double threshold = 0.15;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005";
	private String market = "HK";
	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void backTest() {
		String urlString = "http://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode + "." + market //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";

		Bytes keyBytes = Bytes.of(urlString.getBytes(Constants.charset));
		StoreCache storeCache = new StoreCache();
		URL url = Rethrow.ex(() -> new URL(urlString));

		// Date, Open, High, Low, Close, Volume, Adj Close
		List<String[]> arrays = storeCache //
				.getOutlet(keyBytes, () -> HttpUtil.http("GET", url).out) //
				.collect(BytesUtil.split(Bytes.of((byte) 10))) //
				.skip(1) //
				.map(bytes -> new String(bytes.toBytes(), Constants.charset).split(",")) //
				.sort((a0, a1) -> a0[0].compareTo(a1[0])) //
				.toList();

		String dates[] = Read.from(arrays) //
				.map(array -> array[0]) //
				.toArray(String.class);

		double prices[] = Read.from(arrays) //
				.collect(As.arrayOfDoubles(array -> Double.parseDouble(array[1])));

		// a.collect(As.sequenced((index, array) -> index + "," +
		// String.join(",", array))) .collect(As.joined("\n"));

		validatePrices(prices);

		for (Strategy strategy : Arrays.asList(longHold, movingAverageMeanReverting)) {
			GetBuySell getBuySell = strategy.analyze(prices);
			Account account = new Account();

			for (int day = 0; day < prices.length; day++) {
				int buySell = getBuySell.get(day);
				double price = prices[day];

				account.buySell(buySell, price);

				if (buySell != 0)
					System.out.println("day = " + dates[day] //
							+ ", price = " + price //
							+ ", buy/sell = " + buySell //
							+ ", nLots = " + account.nLots);

				if (Boolean.FALSE) // do not validate yet
					account.validate();
			}

			// sell all stocks at the end
			account.buySell(-account.nLots, prices[prices.length - 1]);

			System.out.println("number of transactions = " + account.nTransactions);
			System.out.println("total net gain = " + account.cash);
		}
	}

	private void validatePrices(double prices[]) {
		double price0 = prices[0];

		for (int i = 1; i < prices.length; i++) {
			double price;

			if ((price = prices[i]) == 0)
				throw new RuntimeException("Price is zero: " + price + "/" + i);

			if (!Double.isFinite(price))
				throw new RuntimeException("Price is not finite: " + price + "/" + i);

			double ratio = (price - price0) / price0;
			if (ratio < -0.8 || 0.8 < ratio)
				throw new RuntimeException("Price varied too much: " + price + "/" + i);
		}
	}

	private class Account {
		private double cash = 0;
		private int nLots = 0;
		private int nTransactions = 0;

		private void buySell(int buySell, double price) {
			cash -= buySell * price;
			nLots += buySell;
			nTransactions += Math.abs(buySell);
		}

		private void validate() {
			if (cash < 0 || nLots < 0)
				throw new RuntimeException("invalid condition");
		}
	}

	private Strategy longHold = prices -> day -> day != 0 ? 0 : 1;

	private Strategy movingAverageMeanReverting = prices -> {
		int nDaysMovingAverage = nPastDays;
		double movingAverages[] = new double[prices.length];
		double movingSum = 0;

		for (int day = 0; day < prices.length; day++) {
			if (nDaysMovingAverage <= day) {
				movingAverages[day] = movingSum / nDaysMovingAverage;
				movingSum -= prices[day - nDaysMovingAverage];
			}
			movingSum += prices[day];
		}

		int signals[] = new int[prices.length];

		for (int day = 0; day < prices.length; day++) {
			int signal;

			if (nPastDays < day && day + nFutureDays < prices.length) {
				double price0 = prices[day];
				double predict = movingAverages[day];
				double ratio = (predict - price0) / price0;

				if (ratio < -threshold)
					signal = -1;
				else if (threshold < ratio)
					signal = 1;
				else
					signal = 0;
			} else
				signal = 0;

			signals[day] = signal;
		}

		// buy/sell if ratio is positive/negative; sell nFutureDays after
		return day -> {
			int signal0 = nFutureDays < day ? -signals[day - nFutureDays] : 0;
			int signal1 = signals[day];
			return signal0 + signal1;
		};
	};

	private interface Strategy {
		public GetBuySell analyze(double prices[]);
	}

	// 1 = buy, 0 = no change, -1 = sell
	private interface GetBuySell {
		public int get(int d);
	}

}
