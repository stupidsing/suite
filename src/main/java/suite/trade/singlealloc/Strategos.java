package suite.trade.singlealloc;

import java.util.Arrays;

import suite.math.DiscreteCosineTransform;
import suite.math.Matrix;
import suite.trade.MovingAverage;
import suite.trade.singlealloc.BuySellStrategy.GetBuySell;
import suite.util.Copy;
import suite.util.To;

public class Strategos {

	private Matrix mtx = new Matrix();
	private MovingAverage ma = new MovingAverage();

	public BuySellStrategy longHold = prices -> day -> day != 0 ? 0 : 1;

	public BuySellStrategy lowPassPrediction(int windowSize, int nFutureDays, int nLowPass, float threshold) {
		DiscreteCosineTransform dct = new DiscreteCosineTransform();
		int nPastDays = windowSize - nFutureDays;

		return prices -> holdFixedDays(prices.length, nFutureDays, day -> {
			if (nPastDays <= day) {
				float[] fs0 = new float[windowSize]; // moving window
				float price0 = prices[day];

				Copy.primitiveArray(prices, day - nPastDays, fs0, 0, nPastDays);
				Arrays.fill(fs0, nPastDays, windowSize, price0);

				float[] fs1 = dct.dct(fs0);
				float[] fs2 = To.arrayOfFloats(windowSize, j -> j < nLowPass ? fs1[j] : 0f);
				float[] fs3 = dct.idct(fs2);

				float predict = fs3[fs3.length - 1];
				return getSignal(price0, predict, threshold);
			} else
				return 0;
		});
	}

	public BuySellStrategy macdSignalLineX(float alpha0, float alpha1, float macdAlpha) {
		return prices -> {
			float[] macd = ma.macd(prices, alpha0, alpha1);
			float[] macdEmas = ma.exponentialMovingAvg(macd, macdAlpha);
			float[] diff = mtx.sub(macd, macdEmas);
			return crossover(diff);
		};
	}

	// trendy; alpha0 < alpha1
	public BuySellStrategy macdZeroLineX(float alpha0, float alpha1) {
		return prices -> crossover(ma.macd(prices, alpha0, alpha1));
	}

	public BuySellStrategy movingAvgMeanReverting(int nPastDays, int nHoldDays, float threshold) {
		return prices -> {
			float[] movingAvgs = ma.movingAvg(prices, nPastDays);

			return holdFixedDays(prices.length, nHoldDays, day -> {
				if (nPastDays <= day) {
					float price0 = prices[day];
					float predict = movingAvgs[day];
					return getSignal(price0, predict, threshold);
				} else
					return 0;
			});
		};
	}

	// buy/sell if ratio is positive/negative; sell/buy nHoldDays after
	private GetBuySell holdFixedDays(int nDays, int nHoldDays, GetBuySell gbs) {
		int[] buySells = To.arrayOfInts(nDays, day -> gbs.get(day));

		return day -> {
			int buySell0 = nHoldDays < day ? -buySells[day - nHoldDays] : 0;
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

	private GetBuySell crossover(float[] diff) {
		return day -> {
			if (0 < day) {
				int signum0 = signum(diff[day - 1]);
				int signum1 = signum(diff[day]);
				return signum0 != signum1 ? signum1 : 0;
			} else
				return 0;
		};
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
