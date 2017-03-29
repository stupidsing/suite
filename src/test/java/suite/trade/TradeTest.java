package suite.trade;

import java.net.URL;
import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import suite.Constants;
import suite.http.HttpUtil;
import suite.os.StoreCache;
import suite.primitive.Bytes;
import suite.primitive.BytesUtil;
import suite.streamlet.As;
import suite.util.Rethrow;

public class TradeTest {

	private double threshold = 0.15;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005";
	private String market = "HK";
	private LocalDate frDate = LocalDate.of(2012, 2, 26);
	private LocalDate toDate = LocalDate.of(2017, 2, 26);

	private interface Strategy {

		// 1 = buy, 0 = no change, -1 = sell
		public GetSignal analyze(double prices[]);
	}

	private interface GetSignal {
		public int signalByDay(int d);
	}

	@Test
	public void backTest() {
		String urlString = "http://chart.finance.yahoo.com/table.csv" //
				+ "?s=" + stockCode + "." + market //
				+ "&a=" + frDate.getMonthValue() + "&b=" + frDate.getDayOfMonth() + "&c=" + frDate.getYear() //
				+ "&d=" + toDate.getMonthValue() + "&e=" + toDate.getDayOfMonth() + "&f=" + toDate.getYear() //
				+ "&g=d&ignore=.csv";

		Bytes keyBytes = Bytes.of(urlString.getBytes(Constants.charset));
		StoreCache sc = new StoreCache();
		URL url = Rethrow.ex(() -> new URL(urlString));

		// Date, Open, High, Low, Close, Volume, Adj Close
		double prices[] = sc.getOutlet(keyBytes, () -> HttpUtil.http("GET", url).out) //
				.collect(BytesUtil.split(Bytes.of((byte) 10))) //
				.skip(1) //
				.map(bytes -> new String(bytes.toBytes(), Constants.charset).split(",")) //
				.sort((a0, a1) -> a0[0].compareTo(a1[0])) //
				.collect(As.arrayOfDoubles(array -> Double.parseDouble(array[1])));

		// a.collect(As.sequenced((index, array) -> index + "," +
		// String.join(",", array))) .collect(As.joined("\n"));

		validatePrices(prices);

		for (Strategy strategy : Arrays.asList(longHold, movingAverageMeanReverting)) {
			GetSignal getSignal = strategy.analyze(prices);

			int nLots = 0;
			int nTransactions = 0;
			double cash = 0;
			int signals[] = new int[prices.length];

			for (int d = 0; d < prices.length; d++) {

				// buy if ratio is positive; sell if ratio is negative
				// sell nFutureDays after
				double price = prices[d];
				int signal0 = nFutureDays < d ? -signals[d - nFutureDays] : 0;
				int signal1 = signals[d] = getSignal.signalByDay(d);
				int buySell = signal0 + signal1;

				nLots += buySell;
				nTransactions += Math.abs(buySell);
				cash += -buySell * price;

				if (buySell != 0)
					System.out.println("d = " + d //
							+ ", price = " + price //
							+ ", buy/sell = " + buySell //
							+ ", nLots = " + nLots);

				if (Boolean.FALSE) // do not validate yet
					if (cash < 0 || nLots < 0)
						throw new RuntimeException("invalid condition");
			}

			// sell all stocks at the end
			if (nLots != 0)
				nTransactions++;
			cash += -nLots * prices[prices.length - 1];

			System.out.println("number of transactions = " + nTransactions);
			System.out.println("total net gain = " + cash);
		}
	}

	private Strategy longHold = prices -> d -> d != 0 ? 0 : 1;

	private Strategy movingAverageMeanReverting = prices -> {
		int nDaysMovingAverage = 64;
		double movingAverages[] = new double[prices.length];
		double movingSum = 0;

		for (int d = 0; d < prices.length; d++) {
			if (nDaysMovingAverage <= d) {
				movingAverages[d] = movingSum / nDaysMovingAverage;
				movingSum -= prices[d - nDaysMovingAverage];
			}
			movingSum += prices[d];
		}

		return day -> {
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

			return signal;
		};
	};

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

}
