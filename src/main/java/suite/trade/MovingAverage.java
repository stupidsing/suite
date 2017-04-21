package suite.trade;

public class MovingAverage {

	public static float[] exponentialMovingAvg(float[] prices, float alpha) {
		float[] emas = new float[prices.length];
		float ema = prices[0];
		for (int day = 0; day < prices.length; day++)
			emas[day] = ema += alpha * (prices[day] - ema);
		return emas;
	}

	public static float[] movingAvg(float[] prices, int windowSize) {
		float[] movingAvgs = new float[prices.length];
		float movingSum = 0f;

		for (int day = 0; day < prices.length; day++) {
			if (windowSize <= day) {
				movingAvgs[day] = movingSum / windowSize;
				movingSum -= prices[day - windowSize];
			}
			movingSum += prices[day];
		}

		return movingAvgs;
	}

}
