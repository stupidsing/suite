package suite.trade;

import java.time.LocalDate;
import java.util.Arrays;

import org.junit.Test;

import suite.os.LogUtil;
import suite.trade.Strategy.GetBuySell;

public class TradeTest {

	private double threshold = 0.15;
	private int nPastDays = 64;
	private int nFutureDays = 8;
	private String stockCode = "0005.HK"; // "JPY%3DX"
	private LocalDate frDate = LocalDate.of(2013, 1, 1);
	private LocalDate toDate = LocalDate.of(2018, 1, 1);

	@Test
	public void backTest() {
		DataSource source = DataSource.yahoo(stockCode, frDate, toDate);
		double prices[] = source.prices;

		validatePrices(prices);

		for (Strategy strategy : Arrays.asList(longHold, movingAverageMeanReverting)) {
			GetBuySell getBuySell = strategy.analyze(prices);
			Account account = new Account();

			for (int day = 0; day < prices.length; day++) {
				int buySell = getBuySell.get(day);
				double price = prices[day];

				account.buySell(buySell, price);

				if (buySell != 0)
					LogUtil.info("" //
							+ "date = " + source.dates[day] //
							+ ", price = " + price //
							+ ", buy/sell = " + buySell //
							+ ", nLots = " + account.nLots());

				if (Boolean.FALSE) // do not validate yet
					account.validate();
			}

			// sell all stocks at the end
			account.buySell(-account.nLots(), prices[prices.length - 1]);

			LogUtil.info("number of transactions = " + account.nTransactions());
			LogUtil.info("total net gain = " + account.cash());
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

}
