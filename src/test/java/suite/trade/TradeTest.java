package suite.trade;

import java.time.LocalDate;

import org.junit.Test;

import suite.math.DiscreteCosineTransform;
import suite.os.LogUtil;

public class TradeTest {

	private float threshold = 0.15f;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005.HK"; // "JPY%3DX"
	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void testBackTest() {
		DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
		source.validate();

		backTest(source, "LowPassMeanReverting", lowPassFilterPrediction);
		backTest(source, "LongHold", longHold);
		backTest(source, "MovingAverageMeanReverting", movingAverageMeanReverting);
	}

	private void backTest(DataSource source, String name, Strategy strategy) {
		BackTest backTest = BackTest.test(source, strategy);
		Account account = backTest.account;
		LogUtil.info("strategy = " + name);
		LogUtil.info("number of transactions = " + account.nTransactions());
		LogUtil.info("total net gain = " + account.cash());
	}

	private Strategy lowPassFilterPrediction = prices -> {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();
		int nDaysMovingWindow = nPastDays;
		int nHistoryDays = nDaysMovingWindow - 8;

		return day -> {
			if (nHistoryDays <= day) {
				float fs0[] = new float[nDaysMovingWindow]; // moving window
				float price0 = prices[day];
				int i = 0;

				for (; i < nHistoryDays; i++)
					fs0[i] = prices[day - nHistoryDays + i];
				for (; i < nDaysMovingWindow; i++)
					fs0[i] = price0;

				float fs1[] = dct.dct(fs0);
				float fs2[] = new float[nDaysMovingWindow];

				for (int j = 0; j < 4; j++)
					fs2[j] = fs1[j];

				float fs3[] = dct.idct(fs2);

				float predict = fs3[fs3.length - 1];
				return getSignal(price0, predict, 0.02f);
			} else
				return 0;
		};
	};

	private Strategy longHold = prices -> day -> day != 0 ? 0 : 1;

	private Strategy movingAverageMeanReverting = prices -> {
		int nDaysMovingWindow = nPastDays;
		float movingAverages[] = new float[prices.length];
		float movingSum = 0;

		for (int day = 0; day < prices.length; day++) {
			if (nDaysMovingWindow <= day) {
				movingAverages[day] = movingSum / nDaysMovingWindow;
				movingSum -= prices[day - nDaysMovingWindow];
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

		// buy/sell if ratio is positive/negative; sell/buy nFutureDays after
		return day -> {
			int signal0 = nFutureDays < day ? -signals[day - nFutureDays] : 0;
			int signal1 = signals[day];
			return signal0 + signal1;
		};
	};

	// get buy/sell signal according to predicted price move direction
	private int getSignal(float price0, float price1, float thr) {
		float ratio = (price1 - price0) / price0;
		int signal;

		if (ratio < -thr)
			signal = -1;
		else if (thr < ratio)
			signal = 1;
		else
			signal = 0;

		return signal;
	}

}
