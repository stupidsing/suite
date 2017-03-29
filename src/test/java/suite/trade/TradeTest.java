package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import suite.os.LogUtil;

public class TradeTest {

	private float threshold = 0.15f;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005.HK"; // "JPY%3DX"
	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void backTest() {
		DataSource source = DataSource.yahoo(stockCode, frDate, toDate);

		source.validate();

		for (Strategy strategy : Arrays.asList(longHold, movingAverageMeanReverting)) {
			BackTest backTest = new BackTest(source, strategy);
			Account account = backTest.account;
			LogUtil.info("number of transactions = " + account.nTransactions());
			LogUtil.info("total net gain = " + account.cash());
		}
	}

	private Strategy longHold = prices -> day -> day != 0 ? 0 : 1;

	private Strategy movingAverageMeanReverting = prices -> {
		int nDaysMovingAverage = nPastDays;
		float movingAverages[] = new float[prices.length];
		float movingSum = 0;

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
				float price0 = prices[day];
				float predict = movingAverages[day];
				float ratio = (predict - price0) / price0;

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

}
