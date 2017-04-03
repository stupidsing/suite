package suite.trade;

import java.util.Arrays;

import suite.math.DiscreteCosineTransform;
import suite.trade.Strategy.GetBuySell;
import suite.util.Copy;

public class Strategos {

	public Strategy lowPassPrediction(int windowSize, int nFutureDays, int nLowPass, float threshold) {
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

	public Strategy longHold = prices -> day -> day != 0 ? 0 : 1;

	// trendy; alpha0 < alpha1
	public Strategy movingAvgConvDiv(float alpha0, float alpha1, float macdAlpha, float threshold) {
		return prices -> {
			float emas0[] = exponentialMovingAvg(prices, alpha0); // long-term
			float emas1[] = exponentialMovingAvg(prices, alpha1); // short-term
			float macd[] = new float[prices.length];

			for (int i = 0; i < prices.length; i++)
				macd[i] = emas1[i] - emas0[i];

			float macdEmas[] = exponentialMovingAvg(macd, macdAlpha);

			return day -> { // zero crossover
				if (0 < day) {
					int signum0 = signum(macd[day - 1]);
					int signum1 = signum(macd[day]);
					return signum0 != signum1 ? signum1 : 0;
				} else
					return 0;
			};
		};
	}

	public Strategy movingAvgMeanReverting(int nPastDays, int nFutureDays, float threshold) {
		return prices -> {
			float movingAvgs[] = movingAvg(prices, nPastDays);
			int buySells[] = new int[prices.length];

			for (int day = 0; day < prices.length; day++) {
				int buySell;

				if (nPastDays <= day) {
					float price0 = prices[day];
					float predict = movingAvgs[day];
					buySell = getSignal(price0, predict, threshold);
				} else
					buySell = 0;

				buySells[day] = buySell;
			}

			return holdFixedDays(nFutureDays, buySells);
		};
	}

	private float[] movingAvg(float prices[], int windowSize) {
		float movingAvgs[] = new float[prices.length];
		float movingSum = 0;

		for (int day = 0; day < prices.length; day++) {
			if (windowSize <= day) {
				movingAvgs[day] = movingSum / windowSize;
				movingSum -= prices[day - windowSize];
			}
			movingSum += prices[day];
		}

		return movingAvgs;
	}

	private float[] exponentialMovingAvg(float prices[], float alpha) {
		float emas[] = new float[prices.length];
		float ema = prices[0];
		for (int day = 0; day < prices.length; day++)
			emas[day] = ema += alpha * (prices[day] - ema);
		return emas;
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

	private int signum(float f) {
		if (f < 0)
			return -1;
		else if (0 < f)
			return 1;
		else
			return 0;
	}

}
