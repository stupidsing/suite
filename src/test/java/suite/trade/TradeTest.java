package suite.trade;

import java.time.LocalDate;

import org.junit.Test;

import suite.adt.Fixie;
import suite.adt.Fixie.D_;
import suite.math.DiscreteCosineTransform;
import suite.os.LogUtil;

public class TradeTest {

	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void testBackTest() {
		for (Fixie<String, String, Integer, D_, D_, D_, D_, D_, D_, D_> stock : new Hkex().hkex)
			try {
				// String stockCode = "0066.HK"; // "JPY%3DX";
				String stockCode = stock.t0 + ".HK";
				DataSource source = DataSource.yahoo(stockCode, frDate, toDate);

				LogUtil.info(stockCode + " " + stock.t1);
				backTest(source, "LowPassMeanReverting", lowPassFilterPrediction(128, 8, 8, 0.02f));
				backTest(source, "LongHold", longHold);
				backTest(source, "MovingAverageMeanReverting", movingAverageMeanReverting(128, 8, 0.15f));
			} catch (Exception ex) {
				LogUtil.warn(ex.getMessage());
			}
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

	private void backTest(DataSource source, String name, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info("" //
				+ "strategy = " + name //
				+ ", number of transactions = " + account.nTransactions() //
				+ ", net gain = " + String.format("%.2f", account.cash()));
	}

	private Strategy lowPassFilterPrediction(int windowSize, int nFutureDays, int nLowPass, float threshold) {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();
		int nPastDays = windowSize - nFutureDays;

		return prices -> day -> {
			if (nPastDays <= day) {
				float fs0[] = new float[windowSize]; // moving window
				float price0 = prices[day];
				int i = 0;

				for (; i < nPastDays; i++)
					fs0[i] = prices[day - nPastDays + i];
				for (; i < windowSize; i++)
					fs0[i] = price0;

				float fs1[] = dct.dct(fs0);
				float fs2[] = new float[windowSize];

				for (int j = 0; j < nLowPass; j++)
					fs2[j] = fs1[j];

				float fs3[] = dct.idct(fs2);

				float predict = fs3[fs3.length - 1];
				return getSignal(price0, predict, threshold);
			} else
				return 0;
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

			int signals[] = new int[prices.length];

			for (int day = 0; day < prices.length; day++) {
				int signal;

				if (nPastDays <= day) {
					float price0 = prices[day];
					float predict = movingAverages[day];
					signal = getSignal(price0, predict, threshold);
				} else
					signal = 0;

				signals[day] = signal;
			}

			// buy/sell if ratio is positive/negative; sell/buy nFutureDays
			// after
			return day -> {
				int signal0 = nFutureDays < day ? -signals[day - nFutureDays] : 0;
				int signal1 = signals[day];
				return signal0 + signal1;
			};
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
