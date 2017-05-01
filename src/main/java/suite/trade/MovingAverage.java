package suite.trade;

import suite.math.Matrix;
import suite.util.To;

public class MovingAverage {

	private Matrix mtx = new Matrix();

	// moving average convergence/divergence
	public float[] macd(float[] prices, float alpha0, float alpha1) {
		float[] emas0 = exponentialMovingAvg(prices, alpha0); // long-term
		float[] emas1 = exponentialMovingAvg(prices, alpha1); // short-term
		return mtx.sub(emas1, emas0);
	}

	public float[] exponentialMovingGeometricAvg(float[] prices, int windowSize) {
		float[] logPrices = To.floatArray(prices, price -> (float) Math.log(price));
		float[] movingAvgs = exponentialMovingAvg(logPrices, windowSize);
		return To.floatArray(movingAvgs, lma -> (float) Math.exp(lma));
	}

	public float[] movingGeometricAvg(float[] prices, int windowSize) {
		float[] logPrices = To.floatArray(prices, price -> (float) Math.log(price));
		float[] movingAvgs = movingAvg(logPrices, windowSize);
		return To.floatArray(movingAvgs, lma -> (float) Math.exp(lma));
	}

	public float[] exponentialMovingAvg(float[] prices, float alpha) {
		float[] emas = new float[prices.length];
		float ema = prices[0];
		for (int day = 0; day < prices.length; day++)
			emas[day] = ema += alpha * (prices[day] - ema);
		return emas;
	}

	public float[] movingAvg(float[] prices, int windowSize) {
		float[] movingAvgs = new float[prices.length];
		float div = 1f / windowSize;
		float movingSum = 0f;

		for (int day = 0; day < prices.length; day++) {
			if (windowSize <= day) {
				movingAvgs[day] = movingSum * div;
				movingSum -= prices[day - windowSize];
			}
			movingSum += prices[day];
		}

		return movingAvgs;
	}

}
