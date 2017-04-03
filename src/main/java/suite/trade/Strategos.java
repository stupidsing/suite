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

	public Strategy movingAvgConvDivSignalLineCrossover(float alpha0, float alpha1, float macdAlpha) {
		return prices -> {
			float macd[] = macd(prices, alpha0, alpha1);
			float macdEmas[] = exponentialMovingAvg(macd, macdAlpha);
			float diff[] = subtract(macd, macdEmas);
			return crossover(diff);
		};
	}

	// trendy; alpha0 < alpha1
	public Strategy movingAvgConvDivZeroCrossover(float alpha0, float alpha1) {
		return prices -> {
			float macd[] = macd(prices, alpha0, alpha1);
			return crossover(macd);
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

	private float[] macd(float[] prices, float alpha0, float alpha1) {
		float emas0[] = exponentialMovingAvg(prices, alpha0); // long-term
		float emas1[] = exponentialMovingAvg(prices, alpha1); // short-term
		return subtract(emas1, emas0);
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

	private GetBuySell crossover(float diff[]) {
		return day -> {
			if (0 < day) {
				int signum0 = signum(diff[day - 1]);
				int signum1 = signum(diff[day]);
				return signum0 != signum1 ? signum1 : 0;
			} else
				return 0;
		};
	}

	private float[] subtract(float a[], float b[]) {
		float c[] = new float[a.length];
		for (int i = 0; i < a.length; i++)
			c[i] = a[i] - b[i];
		return c;
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
