package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.math.DiscreteCosineTransform;
import suite.os.LogUtil;
import suite.trade.Strategy.GetBuySell;
import suite.util.Copy;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void testBackTest() {
		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex) {
			// String stockCode = "0066.HK"; // "JPY%3DX";
			String stockCode = stock.t0 + ".HK";
			String stockName = stock.t1;
			backTestStock(stockCode, stockName);
		}
	}

	@Test
	public void testBackTest5() {
		backTestStock("0066.HK", "HSBC"); // "JPY%3DX";
	}

	@Test
	public void testToday() {
		Strategy strategy = movingAverageMeanReverting(128, 8, 0.15f);

		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex)
			try {
				// String stockCode = "0066.HK"; // "JPY%3DX";
				String stockCode = stock.t0 + ".HK";
				DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
				float prices[] = source.prices;

				int signal = strategy.analyze(prices).get(prices.length - 1);
				if (signal != 0)
					LogUtil.info("equity " + stockCode + " " + stock.t1 + " has signal " + signal);
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage());
			}
	}

	private void backTestStock(String stockCode, String stockName) {
		String prefix = stockCode + " " + stockName;

		try {
			DataSource source = DataSource.yahoo(stockCode, frDate, toDate);

			backTest(source, prefix + ", strategy = LowPassMeanReverting", lowPassFilterPrediction(128, 8, 8, 0.02f));
			backTest(source, prefix + ", strategy = LongHold", longHold);
			backTest(source, prefix + ", strategy = MovingAverageMeanReverting", movingAverageMeanReverting(128, 8, 0.15f));
		} catch (Exception ex) {
			LogUtil.warn(ex.getMessage() + " in " + prefix);
		}
	}

	private void backTest(DataSource source, String prefix, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info(prefix //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
	}

	private Strategy lowPassFilterPrediction(int windowSize, int nFutureDays, int nLowPass, float threshold) {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();
		int nPastDays = windowSize - nFutureDays;

		return prices -> {
			int buySells[] = new int[prices.length];

			for (int day = 0; day < prices.length; day++) {
				int buySell;

				if (nPastDays <= day) {
					float fs0[] = new float[windowSize]; // moving window
					float price0 = prices[day];

					Copy.primitiveArray(prices, day - nPastDays, fs0, 0, nPastDays);
					Arrays.fill(fs0, nPastDays, windowSize, price0);

					float fs1[] = dct.dct(fs0);
					float fs2[] = new float[windowSize];

					for (int j = 0; j < nLowPass; j++)
						fs2[j] = fs1[j];

					float fs3[] = dct.idct(fs2);

					float predict = fs3[fs3.length - 1];
					buySell = getSignal(price0, predict, threshold);
				} else
					buySell = 0;

				buySells[day] = buySell;
			}

			return holdFixedDays(nFutureDays, buySells);
		};
	}

	private Strategy longHold = prices -> day -> day != 0 ? 0 : 1;

	private Strategy movingAverageMeanReverting(int nPastDays, int nFutureDays, float threshold) {
		return prices -> {
			float movingAverages[] = new float[prices.length];
			float movingSum = 0;

			for (int day = 0; day < prices.length; day++) {
				if (nPastDays <= day) {
					movingAverages[day] = movingSum / nPastDays;
					movingSum -= prices[day - nPastDays];
				}
				movingSum += prices[day];
			}

			int buySells[] = new int[prices.length];

			for (int day = 0; day < prices.length; day++) {
				int buySell;

				if (nPastDays <= day) {
					float price0 = prices[day];
					float predict = movingAverages[day];
					buySell = getSignal(price0, predict, threshold);
				} else
					buySell = 0;

				buySells[day] = buySell;
			}

			return holdFixedDays(nFutureDays, buySells);
		};
	}

	// buy/sell if ratio is positive/negative; sell/buy nFutureDays after
	private GetBuySell holdFixedDays(int nFutureDays, int buySells[]) {
		return day -> {
			int buySell0 = nFutureDays < day ? -buySells[day - nFutureDays] : 0;
			int buySell1 = buySells[day];
			return buySell0 + buySell1;
		};
	}

	// get buy/sell signal according to predicted price move direction
	private int getSignal(float price0, float price1, float threshold) {
		float ratio = (price1 - price0) / price0;
		int signal;

		if (ratio < -threshold)
			signal = -1;
		else if (threshold < ratio)
			signal = 1;
		else
			signal = 0;

		return signal;
	}

}
