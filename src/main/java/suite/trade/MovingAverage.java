package suite.trade;

import java.util.Arrays;

import suite.math.linalg.Matrix;
import suite.util.To;

public class MovingAverage {

	private Matrix mtx = new Matrix();

	// moving average convergence/divergence
	public float[] macd(float[] prices, double alpha0, double alpha1) {
		float[] emas0 = exponentialMovingAvg(prices, alpha0); // long-term
		float[] emas1 = exponentialMovingAvg(prices, alpha1); // short-term
		return mtx.sub(emas1, emas0);
	}

	public float[] exponentialMovingGeometricAvg(float[] prices, double alpha) {
		float[] logPrices = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		float[] movingAvgs = exponentialMovingAvg(logPrices, alpha);
		return To.arrayOfFloats(movingAvgs, lma -> (float) Math.exp(lma));
	}

	public float[] movingGeometricAvg(float[] prices, int windowSize) {
		float[] logPrices = To.arrayOfFloats(prices, price -> (float) Math.log(price));
		float[] movingAvgs = movingAvg(logPrices, windowSize);
		return To.arrayOfFloats(movingAvgs, lma -> (float) Math.exp(lma));
	}

	public float[] exponentialMovingAvg(float[] prices, double alpha) {
		float[] emas = new float[prices.length];
		double ema = prices[0];
		for (int day = 0; day < prices.length; day++)
			emas[day] = (float) (ema += alpha * (prices[day] - ema));
		return emas;
	}

	public float[] movingAvg(float[] prices, int windowSize) {
		float[] movingAvgs = new float[prices.length];
		float div = 1f / windowSize;
		float movingSum = prices[0] * windowSize;

		for (int day = 0; day < prices.length; day++) {
			movingSum += prices[day] - prices[Math.max(0, day - windowSize)];
			movingAvgs[day] = movingSum * div;
		}

		return movingAvgs;
	}

	public float[] movingMedian(float[] prices, int windowSize) {
		return To.arrayOfFloats(prices.length, i -> {
			float[] window = Arrays.copyOfRange(prices, Math.max(0, i - windowSize), i);
			if (1 < window.length) {
				Arrays.sort(window);
				return window[window.length / 2];
			} else
				return prices[i];
		});
	}

}
